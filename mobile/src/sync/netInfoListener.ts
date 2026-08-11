import NetInfo, { NetInfoState } from '@react-native-community/netinfo';

type NetworkChangeListener = (isOnline: boolean) => void;

class NetworkMonitorImpl {
  private isOnlineState: boolean = true;
  private listeners: Set<NetworkChangeListener> = new Set();
  private unsubscribeNetInfo: (() => void) | null = null;

  constructor() {
    this.init();
  }

  private init() {
    this.unsubscribeNetInfo = NetInfo.addEventListener((state: NetInfoState) => {
      const online = Boolean(state.isConnected && state.isInternetReachable !== false);
      if (this.isOnlineState !== online) {
        this.isOnlineState = online;
        this.notifyListeners();
      }
    });

    NetInfo.fetch().then((state: NetInfoState) => {
      this.isOnlineState = Boolean(state.isConnected && state.isInternetReachable !== false);
      this.notifyListeners();
    });
  }

  get isOnline(): boolean {
    return this.isOnlineState;
  }

  subscribe(listener: NetworkChangeListener): () => void {
    this.listeners.add(listener);
    listener(this.isOnlineState);
    return () => {
      this.listeners.delete(listener);
    };
  }

  private notifyListeners() {
    this.listeners.forEach((listener) => listener(this.isOnlineState));
  }
}

export const NetworkMonitor = new NetworkMonitorImpl();
