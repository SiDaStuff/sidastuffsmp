const fs = require('fs');
const path = require('path');

const clientEnvKeys = [
  'FIREBASE_API_KEY',
  'FIREBASE_AUTH_DOMAIN',
  'FIREBASE_PROJECT_ID'
];

function injectFirebaseConfig() {
  const firebaseJs = path.join(__dirname, 'js', 'firebase.js');
  let content = fs.readFileSync(firebaseJs, 'utf8');

  const apiKey = process.env.FIREBASE_API_KEY || '';
  const authDomain = process.env.FIREBASE_AUTH_DOMAIN || '';
  const projectId = process.env.FIREBASE_PROJECT_ID || '';

  content = content.replace(
    /var\s+firebaseConfig\s*=\s*\{[\s\S]*?\};/,
    `var firebaseConfig = {
  apiKey: '${apiKey}',
  authDomain: '${authDomain}',
  projectId: '${projectId}'
};`
  );

  fs.writeFileSync(firebaseJs, content, 'utf8');
  console.log('Firebase client config injected into js/firebase.js');
}

function injectIndexHtml() {
  const indexHtml = path.join(__dirname, 'index.html');
  let content = fs.readFileSync(indexHtml, 'utf8');

  // Inject Netlify env script that sets window config
  const injectedScript = `<script>
  window.__ENV__ = {
    API_BASE: '/.netlify/functions/api'
  };
</script>`;

  if (!content.includes('window.__ENV__')) {
    content = content.replace('</head>', injectedScript + '\n</head>');
    fs.writeFileSync(indexHtml, content, 'utf8');
    console.log('API base injected into index.html');
  }
}

injectFirebaseConfig();
injectIndexHtml();
console.log('Redeploy script complete.');
