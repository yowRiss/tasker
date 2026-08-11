type Listener = () => void;

class DatabaseNotifierImpl {
  private listeners: Map<string, Set<Listener>> = new Map();

  subscribe(table: string, listener: Listener): () => void {
    if (!this.listeners.has(table)) {
      this.listeners.set(table, new Set());
    }
    this.listeners.get(table)!.add(listener);

    return () => {
      this.listeners.get(table)?.delete(listener);
    };
  }

  notify(tables: string[]) {
    for (const table of tables) {
      const set = this.listeners.get(table);
      if (set) {
        set.forEach((listener) => listener());
      }
    }
  }
}

export const DatabaseNotifier = new DatabaseNotifierImpl();
