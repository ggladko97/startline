import * as WebBrowser from 'expo-web-browser';
import * as Google from 'expo-auth-session/providers/google';
import { makeRedirectUri } from 'expo-auth-session';
import { Platform } from 'react-native';

WebBrowser.maybeCompleteAuthSession();

export interface GoogleAuthResult {
  idToken: string;
  accessToken: string;
  sub: string;
  email: string;
  name?: string;
  picture?: string;
}

export const useGoogleAuth = () => {
  const redirectUri = makeRedirectUri({
    scheme: 'myapp',
    path: 'redirect',
  });

  const [request, response, promptAsync] = Google.useAuthRequest({
    clientId: process.env.EXPO_PUBLIC_GOOGLE_CLIENT_ID,
    iosClientId: process.env.EXPO_PUBLIC_GOOGLE_IOS_CLIENT_ID,
    androidClientId: process.env.EXPO_PUBLIC_GOOGLE_ANDROID_CLIENT_ID,
    redirectUri,
    scopes: ['openid', 'profile', 'email'],
  });

  return {
    request,
    response,
    promptAsync,
  };
};

export const parseGoogleToken = async (
  token: string
): Promise<GoogleAuthResult | null> => {
  try {
    const response = await fetch(
      `https://www.googleapis.com/oauth2/v3/tokeninfo?id_token=${token}`
    );
    const data = await response.json();

    return {
      idToken: token,
      accessToken: token,
      sub: data.sub,
      email: data.email,
      name: data.name,
      picture: data.picture,
    };
  } catch (error) {
    console.error('Failed to parse Google token:', error);
    return null;
  }
};

export const clearAuth = async () => {
  if (Platform.OS === 'web') {
    await WebBrowser.dismissAuthSession();
  }
};
