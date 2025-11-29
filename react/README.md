# Car Appraisal Platform - Frontend

A production-ready React Native Expo application for the Car Appraisal Platform, supporting both web and mobile platforms.

## Features

- **Google OAuth Authentication** - Secure sign-in with Google accounts
- **Role-Based Access** - Separate interfaces for Clients and Appraisers
- **Order Management** - Create, track, and manage vehicle appraisal orders
- **Real-time Polling** - Appraisers receive new order notifications via polling
- **Report Submission** - Upload photos and generate PDF reports
- **Responsive Design** - Clean, modern UI in dark/purple theme
- **Cross-Platform** - Runs on web, iOS, and Android

## Tech Stack

- **Framework**: React Native + Expo
- **Language**: TypeScript
- **State Management**: React Context + TanStack Query
- **API Client**: Axios
- **Authentication**: Google OAuth 2.0 via expo-auth-session
- **PDF Generation**: pdf-lib
- **Storage**: AsyncStorage
- **Icons**: Lucide React Native

## Prerequisites

- Node.js 18+
- npm or yarn
- Expo CLI
- Backend API running (see backend repository)

## Environment Setup

Create a `.env` file with the following variables:

```env
EXPO_PUBLIC_API_BASE_URL=http://localhost:8080/api/v1
EXPO_PUBLIC_GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
EXPO_PUBLIC_GOOGLE_IOS_CLIENT_ID=your-ios-client-id.apps.googleusercontent.com
EXPO_PUBLIC_GOOGLE_ANDROID_CLIENT_ID=your-android-client-id.apps.googleusercontent.com
```

### Google OAuth Setup

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing
3. Enable Google+ API
4. Create OAuth 2.0 credentials:
   - **Web client**: For web platform
   - **iOS client**: For iOS platform
   - **Android client**: For Android platform
5. Add authorized redirect URIs:
   - Web: `http://localhost:19006`
   - iOS: `myapp://redirect`
   - Android: Configure in `app.json`

## Installation

```bash
# Install dependencies
npm install

# Start development server
npm run dev

# Build for web
npm run build:web

# Type check
npm run typecheck
```

## Project Structure

```
project/
├── app/                    # Expo Router screens
│   ├── (tabs)/            # Tab navigation
│   │   ├── index.tsx      # Home screen
│   │   ├── orders.tsx     # Orders list
│   │   └── profile.tsx    # User profile
│   ├── login.tsx          # Login screen
│   ├── create-order.tsx   # Order creation wizard
│   ├── order/[id].tsx     # Order details
│   └── submit-report/[orderId].tsx  # Report submission
├── components/            # Reusable UI components
│   ├── Button.tsx
│   ├── Card.tsx
│   ├── Input.tsx
│   ├── Banner.tsx
│   └── LoadingScreen.tsx
├── contexts/              # React contexts
│   └── AuthContext.tsx    # Authentication state
├── services/              # API and service layers
│   ├── api.ts             # Backend API client
│   ├── auth.ts            # Google OAuth service
│   └── pdf.ts             # PDF generation
├── types/                 # TypeScript definitions
│   ├── api.ts             # API types
│   └── env.d.ts           # Environment types
└── Dockerfile             # Docker configuration
```

## User Flows

### Client Flow

1. **Login** - Sign in with Google account
2. **Dashboard** - View options to create order or view existing orders
3. **Create Order** - Step-by-step wizard:
   - Enter car make
   - Enter car model
   - Enter car year
   - Enter location
   - Confirm and submit
4. **View Orders** - List of all submitted orders with status
5. **Order Details** - View order status, timeline, and report when available

### Appraiser Flow

1. **Login** - Sign in with Google account (must be whitelisted)
2. **Dashboard** - View available and assigned orders
3. **Orders List** - Auto-polling for new orders every 15 seconds
4. **Accept Order** - Claim available orders
5. **Submit Report**:
   - Upload up to 50 photos
   - Add appraisal description
   - Generate PDF report
   - Submit to client

## API Integration

The app communicates with the backend REST API:

### Authentication
- `POST /users/register` - Register new user
- `GET /users/{externalId}` - Get user by Google sub ID
- `GET /users/me` - Get current user

### Orders
- `POST /orders` - Create new order
- `GET /orders?userId={id}` - Get client orders
- `GET /orders?appraiserId={id}` - Get appraiser orders
- `GET /orders/{id}` - Get order details
- `POST /orders/{id}/assign` - Assign order to appraiser
- `PUT /orders/{id}/status` - Update order status

### Reports
- `POST /reports/orders/{orderId}` - Upload report
- `GET /reports/orders/{orderId}` - Get report

## Docker Deployment

### Build Image

```bash
docker build -t car-appraisal-frontend .
```

### Run Container

```bash
docker run -p 8080:8080 \
  -e EXPO_PUBLIC_API_BASE_URL=http://backend:8080/api/v1 \
  car-appraisal-frontend
```

### Using Docker Compose

```bash
# Start all services (frontend, backend, database)
docker-compose up -d

# View logs
docker-compose logs -f frontend

# Stop services
docker-compose down
```

The frontend will be available at `http://localhost:8080`

## Design System

### Colors

- **Background**: `#111827` (dark gray)
- **Card Background**: `#1F2937` (lighter gray)
- **Primary**: `#6B21A8` (purple)
- **Secondary**: `#F3F4F6` (light gray)
- **Text Primary**: `#F9FAFB` (white)
- **Text Secondary**: `#9CA3AF` (gray)
- **Success**: `#10B981` (green)
- **Error**: `#DC2626` (red)

### Typography

- **Titles**: 24-32px, bold
- **Headings**: 18-20px, semi-bold
- **Body**: 14-16px, regular
- **Small**: 12px, regular

### Components

All components follow a consistent design pattern:
- Rounded corners (12-16px)
- Consistent spacing (8px grid)
- Dark theme throughout
- Smooth transitions

## Development Notes

### Platform-Specific Code

The app uses Expo's managed workflow. For platform-specific features:

```typescript
import { Platform } from 'react-native';

if (Platform.OS !== 'web') {
  // Mobile-only code
}
```

### Error Handling

All API calls include error handling with user-friendly messages:

```typescript
try {
  await apiService.createOrder(data);
} catch (error) {
  setError('Backend is not available');
}
```

### State Management

- **Authentication**: React Context
- **API Caching**: TanStack Query
- **Local Storage**: AsyncStorage

## Testing

```bash
# Run type checking
npm run typecheck

# Lint code
npm run lint
```

## Production Considerations

1. **Environment Variables**: Set production API URL and OAuth credentials
2. **HTTPS**: Enable HTTPS in production
3. **Error Monitoring**: Integrate Sentry or similar
4. **Analytics**: Add analytics tracking
5. **Performance**: Enable code splitting and lazy loading
6. **Security**: Validate all inputs, use secure storage for tokens

## Troubleshooting

### Google Sign-In Issues

- Verify OAuth credentials are correct
- Check redirect URIs are configured
- Ensure Google+ API is enabled

### API Connection Issues

- Verify backend is running
- Check API base URL in `.env`
- Review CORS settings on backend

### Build Issues

- Clear cache: `expo start -c`
- Reinstall dependencies: `rm -rf node_modules && npm install`
- Check Node.js version compatibility

## License

Proprietary - All rights reserved

## Support

For issues or questions, contact the development team.
