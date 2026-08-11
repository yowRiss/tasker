import React, { useState } from 'react';
import { StyleSheet, View, Text } from 'react-native';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { TasksScreen } from '../features/tasks/TasksScreen';
import { NotesScreen } from '../features/notes/NotesScreen';
import { MoneyScreen } from '../features/money/MoneyScreen';
import { SearchScreen } from '../features/search/SearchScreen';
import { SettingsScreen } from '../features/settings/SettingsScreen';
import { SyncStatusBar } from '../features/sync/components/SyncStatusBar';
import { SyncQueueDrawer } from '../features/sync/components/SyncQueueDrawer';
import { useSyncState } from '../features/sync/hooks/useSyncState';
import { colors, typography } from '../theme/tokens';

export type MainTabParamList = {
  Tasks: undefined;
  Notes: undefined;
  Money: undefined;
  Search: undefined;
  Settings: undefined;
};

const Tab = createBottomTabNavigator<MainTabParamList>();

function TabIcon({ name, focused }: { name: string; focused: boolean }) {
  return (
    <View style={styles.iconContainer}>
      <Text style={[styles.iconText, focused && styles.iconTextFocused]}>{name}</Text>
    </View>
  );
}

export const MainTabNavigator: React.FC = () => {
  const { isOnline, isSyncing, pendingCount, failedCount } = useSyncState();
  const [drawerVisible, setDrawerVisible] = useState(false);

  return (
    <View style={styles.flex}>
      <SyncStatusBar
        isOnline={isOnline}
        pendingCount={pendingCount}
        failedCount={failedCount}
        isSyncing={isSyncing}
        onPressStatus={() => setDrawerVisible(true)}
      />
      <Tab.Navigator
        screenOptions={{
          headerShown: false,
          tabBarActiveTintColor: colors.primary,
          tabBarInactiveTintColor: colors.textSubtle,
          tabBarStyle: {
            backgroundColor: colors.bgSurface,
            borderTopColor: colors.border,
            height: 60,
            paddingBottom: 8,
            paddingTop: 8,
          },
          tabBarLabelStyle: {
            fontSize: typography.xs,
            fontWeight: '600',
          },
        }}
      >
        <Tab.Screen
          name="Tasks"
          component={TasksScreen}
          options={{
            tabBarIcon: ({ focused }) => <TabIcon name="✓" focused={focused} />,
          }}
        />
        <Tab.Screen
          name="Notes"
          component={NotesScreen}
          options={{
            tabBarIcon: ({ focused }) => <TabIcon name="✎" focused={focused} />,
          }}
        />
        <Tab.Screen
          name="Money"
          component={MoneyScreen}
          options={{
            tabBarIcon: ({ focused }) => <TabIcon name="$" focused={focused} />,
          }}
        />
        <Tab.Screen
          name="Search"
          component={SearchScreen}
          options={{
            tabBarIcon: ({ focused }) => <TabIcon name="🔍" focused={focused} />,
          }}
        />
        <Tab.Screen
          name="Settings"
          component={SettingsScreen}
          options={{
            tabBarIcon: ({ focused }) => <TabIcon name="⚙" focused={focused} />,
          }}
        />
      </Tab.Navigator>

      <SyncQueueDrawer visible={drawerVisible} onClose={() => setDrawerVisible(false)} />
    </View>
  );
};

const styles = StyleSheet.create({
  flex: {
    flex: 1,
  },
  iconContainer: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  iconText: {
    fontSize: 16,
    color: colors.textSubtle,
  },
  iconTextFocused: {
    color: colors.primary,
    fontWeight: 'bold',
  },
});
