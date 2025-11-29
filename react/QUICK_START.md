# Quick Start Guide

## Setup Steps

1. **Install Dependencies**
   ```bash
   npm install
   ```

2. **Configure Environment Variables**

   Edit `.env` file and add your Google OAuth credentials:
   ```env
   EXPO_PUBLIC_API_BASE_URL=http://localhost:8080/api/v1
   EXPO_PUBLIC_GOOGLE_CLIENT_ID=your-web-client-id.apps.googleusercontent.com
   EXPO_PUBLIC_GOOGLE_IOS_CLIENT_ID=your-ios-client-id.apps.googleusercontent.com
   EXPO_PUBLIC_GOOGLE_ANDROID_CLIENT_ID=your-android-client-id.apps.googleusercontent.com
   ```

3. **Start Development Server**
   ```bash
   npm run dev
   ```

4. **Open the App**
   - Press `w` for web
   - Press `a` for Android (requires Android emulator or device)
   - Press `i` for iOS (requires Mac with Xcode)

## Current Features

### Authentication
- Google OAuth sign-in
- Automatic user registration
- Role-based access (Client/Appraiser)
- Persistent sessions with AsyncStorage

### Screens
- **Login** - Google sign-in
- **Home** - Welcome dashboard with user info
- **Profile** - User details and sign out

## Architecture

```
app/
├── index.tsx              # Entry point with auth routing
├── login.tsx              # Google OAuth login
├── (tabs)/                # Tab navigation
│   ├── index.tsx          # Home screen
│   └── profile.tsx        # Profile screen
contexts/
└── AuthContext.tsx        # Authentication state management
services/
├── api.ts                 # Backend API client
└── auth.ts                # Google OAuth service
components/
└── LoadingScreen.tsx      # Loading indicator
types/
├── api.ts                 # API type definitions
└── env.d.ts               # Environment variables types
```

## Troubleshooting

### "Backend is not available"
- Ensure the backend server is running on `http://localhost:8080`
- Check `EXPO_PUBLIC_API_BASE_URL` in `.env`

### Google Sign-In Not Working
- Verify OAuth credentials in `.env`
- Check redirect URIs in Google Cloud Console
- Ensure Google+ API is enabled

### App Not Loading
- Clear cache: `npx expo start --clear`
- Reinstall dependencies: `rm -rf node_modules && npm install`

## Next Steps

To add more features, you can extend:

1. **Order Management** - Add order creation and listing screens
2. **Report Submission** - Implement photo upload and PDF generation
3. **Real-time Updates** - Add polling for appraisers
4. **Notifications** - Integrate push notifications

See `README.md` for complete documentation.
