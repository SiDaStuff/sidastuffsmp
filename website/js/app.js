function initNav() {
  var navAuth = document.getElementById('navAuth');
  var navUser = document.getElementById('navUser');
  var userEmail = document.getElementById('userEmail');
  var btnLogout = document.getElementById('btnLogout');
  var navOnboard = document.getElementById('navOnboard');

  var updateUI = function(user) {
    if (user) {
      if (navAuth) navAuth.style.display = 'none';
      if (navUser) navUser.style.display = 'flex';
      if (userEmail) userEmail.textContent = user.email || '';
      if (navOnboard) navOnboard.style.display = 'inline';
    } else {
      if (navAuth) navAuth.style.display = 'flex';
      if (navUser) navUser.style.display = 'none';
      if (navOnboard) navOnboard.style.display = 'none';
    }
  };

  if (typeof firebase !== 'undefined' && firebase.auth) {
    firebase.auth().onAuthStateChanged(updateUI);
  }

  if (btnLogout) {
    btnLogout.addEventListener('click', function() {
      if (typeof firebase !== 'undefined' && firebase.auth) {
        firebase.auth().signOut().then(function() {
          window.location.reload();
        });
      }
    });
  }
}

function initServerIpCopy() {
  var ipValue = document.querySelector('.server-ip-value');
  if (!ipValue) return;

  ipValue.addEventListener('click', function() {
    var text = ipValue.textContent.trim();
    if (navigator.clipboard) {
      navigator.clipboard.writeText(text).then(function() {
        ipValue.classList.add('copied');
        ipValue.textContent = 'Copied!';
        setTimeout(function() {
          ipValue.classList.remove('copied');
          ipValue.textContent = text;
        }, 2000);
      });
    } else {
      var ta = document.createElement('textarea');
      ta.value = text;
      document.body.appendChild(ta);
      ta.select();
      document.execCommand('copy');
      document.body.removeChild(ta);
      ipValue.classList.add('copied');
      ipValue.textContent = 'Copied!';
      setTimeout(function() {
        ipValue.classList.remove('copied');
        ipValue.textContent = text;
      }, 2000);
    }
  });
}

function initSearch() {
  var input = document.getElementById('playerSearch');
  var btn = document.getElementById('btnSearch');
  var results = document.getElementById('searchResults');
  var statsPanel = document.getElementById('statsPanel');
  var statsCard = document.getElementById('statsCard');
  var leaderboardBody = document.getElementById('leaderboardBody');

  function escapeHtml(str) {
    var div = document.createElement('div');
    div.textContent = str || '';
    return div.innerHTML;
  }

  function renderStats(playerData) {
    if (!statsPanel || !statsCard) return;
    statsPanel.style.display = 'block';

    var balance = playerData.balance != null ? Number(playerData.balance).toLocaleString() : 'N/A';
    var name = playerData.username || playerData.mcUsername || 'Unknown Player';

    var extra = '';
    var skip = ['username', 'mcUsername', 'balance', 'uuid', 'mcUUID', 'uid', 'email', 'verifyCode', 'linkedAt', 'id'];
    for (var key in playerData) {
      if (skip.indexOf(key) !== -1) continue;
      var val = playerData[key];
      if (val == null) continue;
      extra += '<div class="stat-item"><div class="stat-label">' + escapeHtml(key) + '</div>' +
        '<div class="stat-value">' + escapeHtml(String(val)) + '</div></div>';
    }

    statsCard.innerHTML = '<h3>' + escapeHtml(name) + '</h3>' +
      '<div class="stats-grid">' +
      '<div class="stat-item"><div class="stat-label">Balance</div>' +
      '<div class="stat-balance">' + (balance !== 'N/A' ? '⏣ ' + balance : balance) + '</div></div>' +
      '<div class="stat-item"><div class="stat-label">UUID</div>' +
      '<div class="stat-value" style="font-size:0.8rem;word-break:break-all;">' + escapeHtml(playerData.uuid || playerData.mcUUID || 'N/A') + '</div></div>' +
      extra + '</div>';
  }

  function performSearch(query) {
    var q = query.trim().toLowerCase();
    if (!q) {
      if (results) { results.style.display = 'none'; results.innerHTML = ''; }
      return;
    }

    if (!window.SMP_API) {
      if (results) {
        results.style.display = 'block';
        results.innerHTML = '<div class="search-result-item" style="justify-content:center;color:var(--text-muted);">API not loaded.</div>';
      }
      return;
    }

    if (results) {
      results.style.display = 'block';
      results.innerHTML = '<div class="search-result-item" style="justify-content:center;color:var(--text-muted);">Searching...</div>';
    }

    window.SMP_API.get('/players').then(function(res) {
      var data = res.data;
      if (!data) {
        if (results) results.innerHTML = '<div class="search-result-item" style="justify-content:center;color:var(--text-muted);">No players found.</div>';
        return;
      }

      var items = [];
      for (var key in data) {
        var d = data[key];
        var username = (d.username || d.mcUsername || key).toLowerCase();
        if (username.indexOf(q) !== -1) {
          items.push({ id: key, data: d, label: d.username || d.mcUsername || key });
        }
      }

      if (items.length === 0) {
        if (results) results.innerHTML = '<div class="search-result-item" style="justify-content:center;color:var(--text-muted);">No players found.</div>';
        return;
      }

      if (results) {
        results.innerHTML = items.map(function(item) {
          var bal = item.data.balance != null ? '⏣ ' + Number(item.data.balance).toLocaleString() : '';
          return '<div class="search-result-item" data-key="' + escapeHtml(item.id) + '" data-json="' + escapeHtml(JSON.stringify(item.data)) + '">' +
            '<span class="search-result-name">' + escapeHtml(item.label) + '</span>' +
            (bal ? '<span class="search-result-balance">' + escapeHtml(bal) + '</span>' : '') +
            '</div>';
        }).join('');

        results.querySelectorAll('.search-result-item').forEach(function(el) {
          el.addEventListener('click', function() {
            var json = el.getAttribute('data-json');
            if (json) {
              try { renderStats(JSON.parse(json)); } catch (e) {}
              results.style.display = 'none';
              if (input) input.value = '';
            }
          });
        });
      }
    }).catch(function(err) {
      console.error('Search error:', err);
      if (results) results.innerHTML = '<div class="search-result-item" style="justify-content:center;color:#dc2626;">Search failed.</div>';
    });
  }

  function loadLeaderboard() {
    if (!leaderboardBody) return;

    if (!window.SMP_API) {
      leaderboardBody.innerHTML = '<tr><td colspan="3" class="loading-cell">API not configured.</td></tr>';
      return;
    }

    window.SMP_API.query({
      path: 'players',
      orderBy: 'balance',
      limit: 15,
      descending: true
    }).then(function(items) {
      if (!items || items.length === 0) {
        leaderboardBody.innerHTML = '<tr><td colspan="3" class="loading-cell">No player data yet.</td></tr>';
        return;
      }

      leaderboardBody.innerHTML = '';
      var rank = 0;
      items.forEach(function(item) {
        rank++;
        var name = item.username || item.mcUsername || item.id;
        var bal = item.balance != null ? '⏣ ' + Number(item.balance).toLocaleString() : '—';

        var rankHtml = String(rank);
        if (rank <= 3) {
          rankHtml = '<span class="rank-badge rank-' + rank + '">' + rank + '</span>';
        }

        var tr = document.createElement('tr');
        tr.innerHTML = '<td>' + rankHtml + '</td>' +
          '<td>' + escapeHtml(name) + '</td>' +
          '<td style="font-weight:600;color:var(--teal)">' + escapeHtml(bal) + '</td>';
        tr.style.cursor = 'pointer';
        tr.addEventListener('click', function() {
          renderStats(item);
          window.scrollTo({ top: statsPanel.offsetTop - 80, behavior: 'smooth' });
        });
        leaderboardBody.appendChild(tr);
      });
    }).catch(function(err) {
      console.error('Leaderboard error:', err);
      leaderboardBody.innerHTML = '<tr><td colspan="3" class="loading-cell">Failed to load leaderboard.</td></tr>';
    });
  }

  if (btn && input) {
    btn.addEventListener('click', function() { performSearch(input.value); });
    input.addEventListener('keydown', function(e) { if (e.key === 'Enter') performSearch(input.value); });
    input.addEventListener('input', function() {
      if (input.value.trim().length >= 2) performSearch(input.value);
      else if (results) results.style.display = 'none';
    });
    document.addEventListener('click', function(e) {
      if (results && e.target !== input && e.target !== btn && !results.contains(e.target)) {
        results.style.display = 'none';
      }
    });
  }

  loadLeaderboard();
}

document.addEventListener('DOMContentLoaded', function() {
  initNav();
  initServerIpCopy();
  initSearch();

  [document.getElementById('navLogo'), document.getElementById('heroLogo')].forEach(function(el) {
    if (!el) return;
    el.onerror = function() { el.style.display = 'none'; };
  });
});
