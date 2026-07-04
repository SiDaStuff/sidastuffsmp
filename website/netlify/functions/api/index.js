const admin = require('firebase-admin');

let app = null;

function getApp() {
  if (app) return app;

  let serviceAccount;
  const raw = process.env.SERVICE_ACCOUNT;

  if (raw) {
    try {
      serviceAccount = JSON.parse(raw);
    } catch (e) {
      try {
        serviceAccount = JSON.parse(Buffer.from(raw, 'base64').toString('utf8'));
      } catch (e2) {
        throw new Error('SERVICE_ACCOUNT env is not valid JSON or base64-encoded JSON.');
      }
    }
  }

  if (!serviceAccount || !serviceAccount.project_id) {
    throw new Error('SERVICE_ACCOUNT env is missing or invalid.');
  }

  app = admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
    databaseURL: process.env.FIREBASE_DATABASE_URL || `https://${serviceAccount.project_id}.firebaseio.com`
  });

  return app;
}

const db = () => getApp().database();

const ALLOWED_PATHS = ['players', 'verifyCodes', 'linkedAccounts'];

function validatePath(path) {
  const segments = path.replace(/^\/+|\/+$/g, '').split('/');
  if (!ALLOWED_PATHS.includes(segments[0])) {
    throw new Error('Access denied to path: ' + segments[0]);
  }
}

async function verifyAuth(body) {
  const idToken = body?.idToken;
  if (!idToken) {
    throw new Error('Authentication required');
  }
  const decoded = await getApp().auth().verifyIdToken(idToken);
  return decoded;
}

exports.handler = async function (event) {
  const headers = {
    'Content-Type': 'application/json',
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization',
    'Access-Control-Allow-Methods': 'GET, POST, PUT, PATCH, DELETE, OPTIONS'
  };

  if (event.httpMethod === 'OPTIONS') {
    return { statusCode: 204, headers };
  }

  try {
    getApp();
  } catch (err) {
    return { statusCode: 500, headers, body: JSON.stringify({ error: err.message }) };
  }

  try {
    const body = event.body ? JSON.parse(event.body) : {};
    const method = body?.method || event.httpMethod || 'GET';
    const path = body?.path || '/';
    const data = body?.data;
    const isReadOnly = ['GET', 'QUERY'].includes(method.toUpperCase());

    // Authentication is required for any write operation
    let decodedUser = null;
    if (!isReadOnly) {
      decodedUser = await verifyAuth(body);
    }

    validatePath(path);

    const ref = db().ref(path);
    let result;

    switch (method.toUpperCase()) {
      case 'GET':
        result = await ref.once('value');
        return {
          statusCode: 200,
          headers,
          body: JSON.stringify({ data: result.val() })
        };

      case 'PUT':
        await ref.set(data);
        return { statusCode: 200, headers, body: JSON.stringify({ ok: true, uid: decodedUser.uid }) };

      case 'PATCH':
        await ref.update(data);
        return { statusCode: 200, headers, body: JSON.stringify({ ok: true, uid: decodedUser.uid }) };

      case 'POST':
        const pushRef = ref.push();
        await pushRef.set(data);
        return {
          statusCode: 200,
          headers,
          body: JSON.stringify({ ok: true, key: pushRef.key, uid: decodedUser.uid })
        };

      case 'DELETE':
        await ref.remove();
        return { statusCode: 200, headers, body: JSON.stringify({ ok: true, uid: decodedUser.uid }) };

      case 'QUERY':
        const orderBy = body?.orderBy;
        const limit = body?.limit || 20;
        const startAt = body?.startAt;
        const endAt = body?.endAt;

        let queryRef = ref;
        if (orderBy) queryRef = queryRef.orderByChild(orderBy);
        if (startAt !== undefined) queryRef = queryRef.startAt(startAt);
        if (endAt !== undefined) queryRef = queryRef.endAt(endAt);
        queryRef = queryRef.limitToFirst(limit);

        result = await queryRef.once('value');
        const val = result.val();

        let items = [];
        if (val) {
          items = Object.entries(val).map(([key, value]) => ({ id: key, ...value }));
          if (orderBy && startAt === undefined && endAt === undefined) {
            items.sort((a, b) => {
              const va = a[orderBy], vb = b[orderBy];
              if (typeof va === 'number' && typeof vb === 'number') return vb - va;
              return String(va).localeCompare(String(vb));
            });
          }
          if (body?.descending) items.reverse();
        }

        return {
          statusCode: 200,
          headers,
          body: JSON.stringify({ data: items })
        };

      case 'AUTH':
        return { statusCode: 200, headers, body: JSON.stringify({ uid: decodedUser.uid, email: decodedUser.email }) };

      default:
        return { statusCode: 400, headers, body: JSON.stringify({ error: 'Unknown method: ' + method }) };
    }
  } catch (err) {
    console.error('API error:', err);
    if (err.message === 'Authentication required' || err.message?.includes('verifyIdToken')) {
      return { statusCode: 401, headers, body: JSON.stringify({ error: 'Authentication required' }) };
    }
    return {
      statusCode: err.message?.startsWith('Access denied') ? 403 : 500,
      headers,
      body: JSON.stringify({ error: err.message || 'Internal error' })
    };
  }
};
