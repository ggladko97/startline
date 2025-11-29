import React, { useEffect, useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { useRouter } from 'expo-router';
import { useAuth } from '../contexts/AuthContext';
import { useGoogleAuth, parseGoogleToken } from '../services/auth';
import { LoadingScreen } from '../components/LoadingScreen';

export default function LoginScreen() {
  const router = useRouter();
  const { signIn, isAuthenticated, isLoading } = useAuth();
  const { promptAsync, response } = useGoogleAuth();
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (isAuthenticated) {
      router.replace('/(tabs)');
    }
  }, [isAuthenticated]);

  useEffect(() => {
    handleGoogleResponse();
  }, [response]);

  const handleGoogleResponse = async () => {
    if (response?.type === 'success') {
      try {
        setLoading(true);
        setError(null);

        const { authentication } = response;
        if (!authentication?.idToken) {
          throw new Error('No ID token received');
        }

        const authResult = await parseGoogleToken(authentication.idToken);
        if (!authResult) {
          throw new Error('Failed to parse authentication token');
        }

        await signIn(authResult);
      } catch (err) {
        setError(
          err instanceof Error ? err.message : 'Authentication failed'
        );
      } finally {
        setLoading(false);
      }
    }
  };

  const handleSignIn = async () => {
    try {
      setError(null);
      await promptAsync();
    } catch (err) {
      setError('Failed to initiate sign-in');
    }
  };

  if (isLoading) {
    return <LoadingScreen />;
  }

  return (
    <View style={styles.container}>
      <View style={styles.content}>
        <View style={styles.header}>
          <Text style={styles.title}>Car Appraisal</Text>
          <Text style={styles.subtitle}>
            Professional vehicle inspection and reporting
          </Text>
        </View>

        {error && (
          <View style={styles.errorBanner}>
            <Text style={styles.errorText}>{error}</Text>
          </View>
        )}

        <TouchableOpacity
          style={styles.button}
          onPress={handleSignIn}
          disabled={loading}>
          <Text style={styles.buttonText}>
            {loading ? 'Signing in...' : 'Sign in with Google'}
          </Text>
        </TouchableOpacity>

        <Text style={styles.footer}>
          By signing in, you agree to our terms of service
        </Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#111827',
  },
  content: {
    flex: 1,
    justifyContent: 'center',
    paddingHorizontal: 24,
  },
  header: {
    alignItems: 'center',
    marginBottom: 48,
  },
  title: {
    fontSize: 32,
    fontWeight: '700',
    color: '#F9FAFB',
    marginBottom: 8,
  },
  subtitle: {
    fontSize: 16,
    color: '#9CA3AF',
    textAlign: 'center',
  },
  errorBanner: {
    backgroundColor: '#7F1D1D',
    borderWidth: 1,
    borderColor: '#DC2626',
    padding: 16,
    borderRadius: 12,
    marginBottom: 24,
  },
  errorText: {
    color: '#FCA5A5',
    fontSize: 14,
  },
  button: {
    backgroundColor: '#6B21A8',
    borderRadius: 12,
    paddingVertical: 16,
    paddingHorizontal: 32,
    alignItems: 'center',
    marginTop: 24,
  },
  buttonText: {
    color: '#FFFFFF',
    fontSize: 18,
    fontWeight: '600',
  },
  footer: {
    fontSize: 12,
    color: '#6B7280',
    textAlign: 'center',
    marginTop: 24,
  },
});
