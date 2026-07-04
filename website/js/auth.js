function showError(el, message) {
  if (!el) return;
  el.textContent = message;
  el.classList.remove('form-status');
  el.classList.add('form-error');
  el.style.display = 'block';
}

function clearError(el) {
  if (!el) return;
  el.textContent = '';
  el.style.display = 'none';
}

function mapAuthError(code) {
  var map = {
    'auth/email-already-in-use': 'An account with this email already exists.',
    'auth/invalid-email': 'Please enter a valid email address.',
    'auth/invalid-credential': 'Invalid email or password.',
    'auth/user-not-found': 'No account found with this email.',
    'auth/wrong-password': 'Incorrect password.',
    'auth/weak-password': 'Password must be at least 6 characters.',
    'auth/too-many-requests': 'Too many attempts. Please try again later.',
    'auth/network-request-failed': 'Network error. Check your connection.'
  };
  return map[code] || 'An unexpected error occurred. Please try again.';
}

function initAuthPage() {
  if (typeof firebase === 'undefined' || !firebase.auth) {
    var err = document.getElementById('loginError') || document.getElementById('signupError');
    if (err) showError(err, 'Firebase not loaded.');
    return;
  }

  var loginForm = document.getElementById('loginForm');
  var signupForm = document.getElementById('signupForm');

  if (loginForm) {
    loginForm.addEventListener('submit', function(e) {
      e.preventDefault();
      var email = document.getElementById('email').value.trim();
      var password = document.getElementById('password').value;
      var errorEl = document.getElementById('loginError');
      clearError(errorEl);

      firebase.auth().signInWithEmailAndPassword(email, password)
        .then(function() { window.location.href = '/'; })
        .catch(function(err) { showError(errorEl, mapAuthError(err.code)); });
    });
  }

  if (signupForm) {
    signupForm.addEventListener('submit', function(e) {
      e.preventDefault();
      var email = document.getElementById('email').value.trim();
      var password = document.getElementById('password').value;
      var confirm = document.getElementById('confirmPassword').value;
      var errorEl = document.getElementById('signupError');
      clearError(errorEl);

      if (password !== confirm) {
        showError(errorEl, 'Passwords do not match.');
        return;
      }

      firebase.auth().createUserWithEmailAndPassword(email, password)
        .then(function() { window.location.href = '/onboard.html'; })
        .catch(function(err) { showError(errorEl, mapAuthError(err.code)); });
    });
  }
}

document.addEventListener('DOMContentLoaded', initAuthPage);
