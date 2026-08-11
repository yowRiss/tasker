import * as SecureStore from 'expo-secure-store';

const TOKEN_KEY = 'tasker_jwt_token';
const API_URL_KEY = 'tasker_api_url';
export const DEFAULT_API_URL = 'http://10.0.2.2:8080'; // Android emulator localhost alias

export class AuthService {
  private static cachedToken: string | null = null;
  private static cachedApiUrl: string | null = null;

  static async getToken(): Promise<string | null> {
    if (this.cachedToken) return this.cachedToken;
    try {
      this.cachedToken = await SecureStore.getItemAsync(TOKEN_KEY);
      return this.cachedToken;
    } catch {
      return null;
    }
  }

  static async setToken(token: string): Promise<void> {
    this.cachedToken = token;
    await SecureStore.setItemAsync(TOKEN_KEY, token);
  }

  static async clearToken(): Promise<void> {
    this.cachedToken = null;
    await SecureStore.deleteItemAsync(TOKEN_KEY);
  }

  static async getApiUrl(): Promise<string> {
    if (this.cachedApiUrl) return this.cachedApiUrl;
    try {
      const stored = await SecureStore.getItemAsync(API_URL_KEY);
      this.cachedApiUrl = stored || DEFAULT_API_URL;
      return this.cachedApiUrl;
    } catch {
      return DEFAULT_API_URL;
    }
  }

  static async setApiUrl(url: string): Promise<void> {
    const trimmed = url.trim().replace(/\/+$/, '');
    this.cachedApiUrl = trimmed;
    await SecureStore.setItemAsync(API_URL_KEY, trimmed);
  }
}
