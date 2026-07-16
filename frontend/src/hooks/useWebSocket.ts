import { useEffect, useRef, useCallback } from "react";
import { getStoredTokens } from "../stores/authStore";

type MessageHandler = (data: any) => void;

interface UseWebSocketOptions {
  executionId?: string;
  onExecutionStarted?: MessageHandler;
  onExecutionCompleted?: MessageHandler;
  onExecutionFailed?: MessageHandler;
  onStepStarted?: MessageHandler;
  onStepCompleted?: MessageHandler;
  onStepFailed?: MessageHandler;
  onMessage?: MessageHandler;
}

const WS_BASE = import.meta.env.VITE_WS_URL || "ws://localhost:8080/ws";

export function useWebSocket(options: UseWebSocketOptions) {
  const wsRef = useRef<WebSocket | null>(null);
  const reconnectTimeoutRef = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);
  const { executionId } = options;

  const connect = useCallback(() => {
    if (!executionId) return;

    const tokens = getStoredTokens();
    const url = `${WS_BASE}/executions/${executionId}?token=${tokens.accessToken || ""}`;

    const ws = new WebSocket(url);
    wsRef.current = ws;

    ws.onopen = () => {
      console.log("WebSocket connected for execution:", executionId);
    };

    ws.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data);
        const { type, payload } = message;

        options.onMessage?.(message);

        switch (type) {
          case "EXECUTION_STARTED":
            options.onExecutionStarted?.(payload);
            break;
          case "EXECUTION_COMPLETED":
            options.onExecutionCompleted?.(payload);
            break;
          case "EXECUTION_FAILED":
            options.onExecutionFailed?.(payload);
            break;
          case "STEP_STARTED":
            options.onStepStarted?.(payload);
            break;
          case "STEP_COMPLETED":
            options.onStepCompleted?.(payload);
            break;
          case "STEP_FAILED":
            options.onStepFailed?.(payload);
            break;
        }
      } catch (e) {
        console.error("WebSocket message parse error:", e);
      }
    };

    ws.onclose = () => {
      console.log("WebSocket disconnected");
      reconnectTimeoutRef.current = setTimeout(connect, 3000);
    };

    ws.onerror = (error) => {
      console.error("WebSocket error:", error);
      ws.close();
    };
  }, [executionId, options]);

  useEffect(() => {
    connect();
    return () => {
      if (reconnectTimeoutRef.current) clearTimeout(reconnectTimeoutRef.current);
      if (wsRef.current) {
        wsRef.current.onclose = null;
        wsRef.current.close();
      }
    };
  }, [connect]);

  return {
    send: (data: Record<string, unknown>) => {
      if (wsRef.current?.readyState === WebSocket.OPEN) {
        wsRef.current.send(JSON.stringify(data));
      }
    },
    disconnect: () => {
      if (reconnectTimeoutRef.current) clearTimeout(reconnectTimeoutRef.current);
      if (wsRef.current) {
        wsRef.current.onclose = null;
        wsRef.current.close();
      }
    },
  };
}
