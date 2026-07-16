import { useState, useEffect, useCallback } from "react";
import { api } from "../lib/api";

interface NotificationState {
  notifications: any[];
  unreadCount: number;
  isLoading: boolean;
}

export function useNotifications() {
  const [state, setState] = useState<NotificationState>({
    notifications: [],
    unreadCount: 0,
    isLoading: true,
  });

  const fetchNotifications = useCallback(async (page = 0) => {
    try {
      const [notifRes, unreadRes] = await Promise.all([
        api.getNotifications({ page, size: 20 }),
        api.getUnreadCount(),
      ]);
      setState({
        notifications: notifRes.content,
        unreadCount: unreadRes.count,
        isLoading: false,
      });
    } catch {
      setState((s) => ({ ...s, isLoading: false }));
    }
  }, []);

  const markAsRead = useCallback(async (id: string) => {
    try {
      await api.markNotificationRead(id);
      setState((s) => ({
        ...s,
        notifications: s.notifications.map((n) => (n.id === id ? { ...n, read: true } : n)),
        unreadCount: Math.max(0, s.unreadCount - 1),
      }));
    } catch {}
  }, []);

  useEffect(() => {
    fetchNotifications();
    const interval = setInterval(() => fetchNotifications(), 30000);
    return () => clearInterval(interval);
  }, [fetchNotifications]);

  return { ...state, fetchNotifications, markAsRead };
}
