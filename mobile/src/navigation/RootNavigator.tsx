import React from 'react';
import { StyleSheet, View, ActivityIndicator } from 'react-native';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { useAuth } from '../features/auth/AuthContext';
import { LoginScreen } from '../features/auth/LoginScreen';
import { MainTabNavigator } from './MainTabNavigator';
import { colors } from '../theme/tokens';

const Stack = createNativeStackNavigator();

export const RootNavigator: React.FC = () => {
  const { authState } = useAuth();

  if (authState === 'checking') {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color={colors.primary} />
      </View>
    );
  }

  return (
    <NavigationContainer>
      <Stack.Navigator screenOptions={{ headerShown: false }}>
        {authState === 'authenticated' ? (
          <Stack.Screen name="Main" component={MainTabNavigator} />
        ) : (
          <Stack.Screen name="Login" component={LoginScreen} />
        )}
      </Stack.Navigator>
    </NavigationContainer>
  );
};

const styles = StyleSheet.create({
  loadingContainer: {
    flex: 1,
    backgroundColor: colors.bgPage,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
