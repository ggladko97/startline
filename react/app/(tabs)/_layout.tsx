import { Tabs, Redirect } from 'expo-router';
import { Home, ClipboardList, User } from 'lucide-react-native';
import { useAuth } from '../../contexts/AuthContext';
import { LoadingScreen } from '../../components/LoadingScreen';
import { UserRole } from '../../types/api';

export default function TabLayout() {
  const { user, isLoading, isAuthenticated } = useAuth();

  if (isLoading) {
    return <LoadingScreen />;
  }

  if (!isAuthenticated) {
    return <Redirect href="/login" />;
  }

  const isAppraiser = user?.role === UserRole.APPRAISER;

  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarStyle: {
          backgroundColor: '#1F2937',
          borderTopColor: '#374151',
          borderTopWidth: 1,
        },
        tabBarActiveTintColor: '#A855F7',
        tabBarInactiveTintColor: '#9CA3AF',
      }}>
      <Tabs.Screen
        name="index"
        options={{
          title: 'Home',
          tabBarIcon: ({ size, color }) => <Home size={size} color={color} />,
        }}
      />
      <Tabs.Screen
        name="profile"
        options={{
          title: 'Profile',
          tabBarIcon: ({ size, color }) => <User size={size} color={color} />,
        }}
      />
    </Tabs>
  );
}
