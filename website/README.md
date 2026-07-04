# SiDaStuff SMP Website - Netlify Deployment

## Required Environment Variables (Netlify Dashboard → Site Settings → Environment Variables)

| Variable | Description | Sensitive |
|----------|-------------|-----------|
| `FIREBASE_API_KEY` | Firebase Web API Key | No |
| `FIREBASE_AUTH_DOMAIN` | Firebase Auth Domain (e.g., `project-id.firebaseapp.com`) | No |
| `FIREBASE_PROJECT_ID` | Firebase Project ID | No |
| `FIREBASE_DATABASE_URL` | Realtime Database URL (e.g., `https://project-id.firebaseio.com`) | No |
| `SERVICE_ACCOUNT` | **Base64-encoded** Firebase Service Account JSON | **Yes** |

## Setup Steps

### 1. Create Firebase Service Account
1. Go to Firebase Console → Project Settings → Service Accounts
2. Click "Generate New Private Key"
3. Download the JSON file
4. Base64 encode it: `base64 -i service-account.json` (or use an online tool)
5. Paste the base64 string as `SERVICE_ACCOUNT` in Netlify env vars

### 2. Configure Netlify
- Build command: `node redeploy.js`
- Publish directory: `.` (root of website folder)
- Functions directory: `netlify/functions` (configured in netlify.toml)

### 3. Firebase Realtime Database Rules
Deploy the rules from `database.rules.json`:
```bash
firebase deploy --only database
```

### 4. Server Plugin Configuration (`firebase.yml`)
```yaml
firebase:
  enabled: true
  service-account-json: "firebase-service-account.json"
  database-url: "https://your-project.firebaseio.com"
  stats-sync-path: "player_stats"
  economy-sync-path: "players"
```

Place the service account JSON file in the plugin's data folder or reference an absolute path.

## Data Structure (RTDB)

```
players/
  {username_lower}/
    username: "PlayerName"
    balance: 12345
    uuid: "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
    online: true
    lastSeen: 1234567890

verifyCodes/
  {6-char-code}/
    mcUsername: "PlayerName"
    mcUUID: "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
    createdAt: 1234567890
    expiresAt: 1234567890
    claimed: false
    claimedBy: ""

linkedAccounts/
  {firebase-uid}/
    uid: "firebase-uid"
    email: "user@example.com"
    mcUsername: "PlayerName"
    mcUUID: "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
    linkedAt: {".sv": "timestamp"}
    verifyCode: "ABC123"
```

## Server Commands

- `/verify` - Generates a 6-char code, expires in 5 minutes, writes to RTDB
- Website: User enters code on `/onboard.html` to link their Minecraft account

## API Endpoints (Netlify Functions)

- `GET /.netlify/functions/api?path=players&method=GET` - Read all players
- `GET /.netlify/functions/api?path=players&method=QUERY&orderBy=balance&limit=15&descending=true` - Leaderboard
- `POST /.netlify/functions/api` with `{path, method, data, idToken}` - Write operations (auth required)

## Development

```bash
cd website
npm install
npm run build
```

## File Structure

```
website/
├── index.html          # Homepage - stats search + leaderboard
├── login.html          # Firebase Auth login
├── signup.html         # Firebase Auth signup
├── onboard.html        # Link Minecraft account via /verify code
├── netlify.toml        # Netlify config
├── database.rules.json # RTDB security rules
├── redeploy.js         # Build-time env injection
├── css/style.css       # Styling (palette: #68d8d6, #c4fff9, #9ceaef, #fdfbf7)
├── js/
│   ├── firebase.js     # Firebase client init (build-time injected)
│   ├── api.js          # Netlify Function API client
│   ├── app.js          # Homepage logic
│   ├── auth.js         # Login/signup logic
│   └── onboard.js      # Account linking logic
├── netlify/functions/
│   ├── api/index.js    # Serverless API (Firebase Admin SDK)
│   └── package.json    # firebase-admin dependency
└── assets/logo.png     # Place your logo here
```