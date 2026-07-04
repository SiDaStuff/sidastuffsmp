(function() {
  'use strict';

  var API_BASE = '/.netlify/functions/api';

  function getIdToken() {
    return new Promise(function(resolve, reject) {
      if (typeof firebase === 'undefined' || !firebase.auth) {
        reject(new Error('helper'));
        return;
      }
      var user = firebase.auth().currentUser;
      if (user) {
        user.getIdToken(true).then(resolve).catch(reject);
      } else {
        firebase.auth().onAuthStateChanged(function(u) {
          if (u) {
            u.getIdToken(true).then(resolve).catch(reject);
          } else {
            reject(new Error('helper'));
          }
        });
      }
    });
  }

  function makeRequest(path, method, data, authToken) {
    return new Promise(function(resolve, reject) {
      var xhr = new XMLHttpRequest();
      xhr.open('POST', API_BASE, true);
      xhr.setRequestHeader('Content-Type', 'application/json');
      xhr.timeout = 30000;

      xhr.onload = function() {
        try {
          var res = JSON.parse(xhr.responseText);
          if (xhr.status >= 200 && xhr.status < 300) {
            resolve(res);
          } else {
            reject(new Error(res.error || 'Request failed with status ' + xhr.status));
          }
        } catch (e) {
          reject(new Error('Invalid response: ' + xhr.responseText));
        }
      };

      xhr.onerror = function() { reject(new Error('Network error')); };
      xhr.ontimeout = function() { reject(new Error('Request timed out')); };

      var body = { path: path, method: method || 'GET' };
      if (data !== undefined && data !== null) body.data = data;
      if (authToken) body.idToken = authToken;

      xhr.send(JSON.stringify(body));
    });
  }

  function makeAuthedRequest(path, method, data) {
    return new Promise(function(resolve, reject) {
      getIdToken().then(function(token) {
        makeRequest(path, method, data, token).then(resolve).catch(reject);
      }).catch(function() {
        reject(new Error('Authentication required. Please log in.'));
      });
    });
  }

  window.SMP_API = {
    base: API_BASE,

    get: function(path) {
      return makeRequest(path, 'GET', null, null);
    },

    put: function(path, data) {
      return makeAuthedRequest(path, 'PUT', data);
    },

    patch: function(path, data) {
      return makeAuthedRequest(path, 'PATCH', data);
    },

    post: function(path, data) {
      return makeAuthedRequest(path, 'POST', data);
    },

    delete: function(path) {
      return makeAuthedRequest(path, 'DELETE', null);
    },

    query: function(opts) {
      return new Promise(function(resolve, reject) {
        var body = {
          path: opts.path || '/',
          method: 'QUERY',
          orderBy: opts.orderBy || null,
          limit: opts.limit || 20,
          startAt: opts.startAt || null,
          endAt: opts.endAt || null,
          descending: opts.descending || false
        };
        makeRequest(opts.path, 'QUERY', body, null)
          .then(function(res) { resolve(res.data || []); })
          .catch(reject);
      });
    }
  };
})();
