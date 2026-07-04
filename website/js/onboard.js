function showStatus(message, isError) {
  var statusEl = document.getElementById('onboardStatus');
  var errorEl = document.getElementById('onboardError');
  if (isError) {
    if (errorEl) { errorEl.textContent = message; errorEl.style.display = 'block'; }
    if (statusEl) statusEl.style.display = 'none';
  } else {
    if (statusEl) { statusEl.textContent = message; statusEl.style.display = 'block'; }
    if (errorEl) errorEl.style.display = 'none';
  }
}

function escapeHtml(str) {
  var div = document.createElement('div');
  div.textContent = str || '';
  return div.innerHTML;
}

function initOnboard() {
  var form = document.getElementById('onboardForm');
  var statusEl = document.getElementById('onboardStatus');
  var errorEl = document.getElementById('onboardError');
  var loginPrompt = document.getElementById('loginPrompt');
  var btnLink = document.getElementById('btnLink');
  var navUuser = document.getElementById('userEmail');

  function updateUI(user) {
    if (user) {
      if (loginPrompt) loginPrompt.style.display = 'none';
      if (form) form.style.display = 'block';
      if (navUuser) navUuser.textContent = user.email || '';
    } else {
      if (loginPrompt) loginPrompt.style.display = 'block';
      if (form) form.style.display = 'none';
    }
  }

  if (typeof firebase !== 'undefined' && firebase.auth) {
    firebase.auth().onAuthStateChanged(updateUI);
  }

  if (!form || !btnLink || !window.SMP_API) return;

  form.addEventListener('submit', function(e) {
    e.preventDefault();
    var mcUsername = document.getElementById('mcUsername').value.trim();
    var verifyCode = document.getElementById('verifyCode').value.trim();

    if (errorEl) { errorEl.style.display = 'none'; }
    if (statusEl) { statusEl.style.display = 'none'; }
    btnLink.disabled = true;
    btnLink.textContent = 'Linking...';

    if (!mcUsername || !verifyCode) {
      showStatus('Please fill in all fields.', true);
      btnLink.disabled = false;
      btnLink.textContent = 'Link Account';
      return;
    }

    window.SMP_API.get('verifyCodes/' + verifyCode).then(function(res) {
      var codeData = res.data;
      if (!codeData) {
        throw new Error('Invalid verification code. Make sure you copied it correctly from /verify.');
      }
      if (codeData.claimed) {
        throw new Error('This code has already been used by another account.');
      }
      if (codeData.expiresAt && Date.now() > codeData.expiresAt) {
        throw new Error('This verification code has expired. Run /verify again on the server.');
      }

      var user = typeof firebase !== 'undefined' && firebase.auth ? firebase.auth().currentUser : null;
      if (!user) {
        throw new Error('You must be logged in to link your account.');
      }

      return window.SMP_API.patch('verifyCodes/' + verifyCode, {
        claimed: true,
        claimedBy: user.uid,
        claimedAt: { '.sv': 'timestamp' }
      }).then(function() {
        return window.SMP_API.put('linkedAccounts/' + user.uid, {
          uid: user.uid,
          email: user.email,
          mcUsername: mcUsername,
          mcUUID: codeData.mcUUID || null,
          linkedAt: { '.sv': 'timestamp' },
          verifyCode: verifyCode
        });
      }).then(function() {
        showStatus('Account linked successfully! Your Minecraft username (' + escapeHtml(mcUsername) + ') is now connected.', false);
        form.reset();
      });
    }).catch(function(err) {
      console.error('Link error:', err);
      var msg = err.message || 'Failed to link account. Please try again.';
      if (typeof err === 'string') msg = err;
      showStatus(msg, true);
    }).finally(function() {
      btnLink.disabled = false;
      btnLink.textContent = 'Link Account';
    });
  });
}

document.addEventListener('DOMContentLoaded', initOnboard);
