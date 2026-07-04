var firebaseConfig = {
  apiKey: '__FIREBASE_API_KEY__',
  authDomain: '__FIREBASE_AUTH_DOMAIN__',
  projectId: '__FIREBASE_PROJECT_ID__'
};

var firebaseApp = null;
try {
  if (typeof firebase !== 'undefined') {
    firebaseApp = firebase.initializeApp(firebaseConfig);
  }
} catch (e) {}

var FIREBASE_READY = typeof firebaseApp !== 'undefined' &&
  firebaseConfig.apiKey &&
  firebaseConfig.apiKey.length > 0 &&
  !firebaseConfig.apiKey.startsWith('__');

if (typeof firebase !== 'undefined' && firebase.auth) {
  firebase.auth().onAuthStateChanged(function(user) {
    window.currentUser = user;
  });
}

function getIdToken() {
  return new Promise(function(resolve, reject) {
    if (!window.currentUser) {
      firebase.auth().onAuthStateChanged(function(user) {
        window.currentUser = user;
        if (user) {
          user.getIdToken(true).then(resolve).catch(reject);
        } else {
          reject(new Error('No user logged in'));
        }
      });
    } else {
      window.currentUser.getIdToken(true).then(resolve).catch(reject);
    }
  });
}
