const { GoogleAuth } = require('google-auth-library');

let authClient = null;
let databaseUrl = null;

function getAuthClient() {
  if (authClient) return authClient;

  const raw = process.env.SERVICE_ACCOUNT;
  if (!raw) throw new Error('SERVICE_ACCOUNT env is missing');

  let serviceAccount;
  try {
    serviceAccount = JSON.parse(raw);
  } catch (e) {
    try {
      serviceAccount = JSON.parse(Buffer.from(raw, 'base64').toString('utf8'));
    } catch (e2) {
      throw new Error('SERVICE_ACCOUNT must be valid JSON or base64-encoded JSON');
    }
  }

  if (!serviceAccount.project_id) {
    throw new Error('SERVICE_ACCOUNT missing project_id');
  }

  databaseUrl = process.env.FIREBASE_DATABASE_URL || `https://${serviceAccount.project_id}.firebaseio.com`;

  authClient = new GoogleAuth({
    credentials: serviceAccount,
    scopes: ['https://www.googleapis.com/auth/firebase.database', 'https://www.googleapis.com/auth/userinfo.email']
  });

  return authClient;
}

async function getAccessToken() {
  const client = getAuthClient();
  const accessToken = await client.getAccessToken();
  if (!accessToken) throw new Error('Failed to obtain access token');
  return accessToken;
}

const ALLOWED_PATHS = ['players', 'verifyCodes', 'linkedAccounts', 'player_stats'];

function validatePath(path) {
  const segments = path.replace(/^\/+|\/+$/g, '').split('/');
  if (!ALLOWED_PATHS.includes(segments[0])) {
    throw new Error('Access denied to path: ' + segments[0]);
  }
}

async function requestWithAuth(method, path, data = null) {
  const token = await getAccessToken();
  const url = `${databaseUrl.replace(/\/$/, '')}/${path.replace(/^\/+/, '')}.json`;

  const options = {
    method,
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    }
  };

  if (data !== null) {
    options.body = JSON.stringify(data);
  }

  const response = await fetch(url, options);
  const text = await response.text();

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}: ${text}`);
  }

  return text ? JSON.parse(text) : null;
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
    getAuthClient(); // validate config early
  } catch (err) {
    return { statusCode: 500, headers, body: JSON.stringify({ error: err.message }) };
  }

  try {
    const body = event.body ? JSON.parse(event.body) : {};
    const method = (body?.toUpperCase();
    const path = body.path || '/';
    const data = body.data;
    const isReadOnly = ['GET', 'QUERY'].includes(method);

    if (!isReadOnly) {
      // For write operations, we could verify a client-provided idToken here if needed
      // but since this is server-to-server via service account, we trust the function itself
    }

    validatePath(path);

    let result;

    switch (method) {
      case 'GET':
        result = await requestWithAuth('GET', path);
        return { statusCode: 200, headers, body: JSON.stringify({ data: result }) };

      case 'PUT':
        await requestWithAuth('PUT', path, data);
        return { statusCode: 200, headers, body: JSON.stringify({ ok: true }) };

      case 'PATCH':
        await requestWithAuth('PATCH', path, data);
        return { statusCode: 200, headers, body: JSON.stringify({ ok: true }) };

      case 'POST': {
        const pushPath = path.replace(/\/$/, '') + '/' + Math.random().toString(36).substring(2, 15);
        await requestWithAuth('PUT', pushPath, data);
        return { statusCode: 200, headers, body: JSON.stringify({ ok: true, key: pushPath.split('/').pop() }) };
      }

      case 'DELETE':
        await requestWithAuth('DELETE', path);
        return { statusCode: 200, headers, body: JSON.stringify({ ok: true }) };

      case 'QUERY': {
        const orderBy = body.orderBy;
        const limit = body.limit || 20;
        const startAt = body.startAt;
        const endAt = body.endAt;

        // Use REST API query parameters
        let url = `${databaseUrl.replace(/\/$/, '')}/${path.replace(/^\/+/, '')}.json`;
        const params = new URLSearchParams();
        if (orderBy) params.set('orderBy', `"${orderBy}"`);
        params.set('limitToFirst', String(limit));
        if (startAt !== undefined) params.set('startAt', JSON.stringify(startAt));
        if (endAt !== undefined) params.set('endAt', JSON.stringify(endAt));
        url += '?' + params.toString();

        const token = await getAccessToken();
        const resp = await fetch(url, {
          headers: { 'Authorization': `Bearer ${token}` }
        });
        const text = await resp.text();
        if (!resp.ok) throw new Error(`HTTP ${resp.status}: ${text}`);

        const val = text ? JSON.parse(text) : null;
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
          if (body.descending) items.reverse();
        }
        return { statusCode: 200, headers, body: JSON.stringify({ data: items }) };
      }

      default:
        return { statusCode: 400, headers, body: JSON.stringify({ error: 'Unknown method: ' + method }) };
    }
  } catch (err) {
    console.error('API error:', err);
    const status = err.message?.includes('Access denied') ? 403 : (err.message?.startsWith('HTTP 401') ? 401 : 500);
    return { statusCode: status, headers, body: JSON.stringify({ error: err.message || 'Internal error' }) };
  }
};