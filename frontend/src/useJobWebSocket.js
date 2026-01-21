import { useEffect, useRef } from "react";
import { Client } from "@stomp/stompjs";

const WS_URL = "ws://localhost:8080/ws";

export function useJobWebSocket({ tenantId, onJobEvent, onSummaryEvent }) {
    const clientRef = useRef(null);

    useEffect(() => {
        if (!tenantId) return;

        const client = new Client({
            brokerURL: WS_URL,
            reconnectDelay: 2000,
            debug: () => { }, // set to console.log if you want verbose logs
            onConnect: () => {
                // Subscribe to JOB events
                client.subscribe(`/topic/tenant/${tenantId}/jobs`, (message) => {
                    try {
                        const body = JSON.parse(message.body);
                        onJobEvent?.(body);
                    } catch (e) {
                        console.error("Failed parsing job event", e);
                    }
                });

                // Subscribe to SUMMARY events
                client.subscribe(`/topic/tenant/${tenantId}/summary`, (message) => {
                    try {
                        const body = JSON.parse(message.body);
                        onSummaryEvent?.(body);
                    } catch (e) {
                        console.error("Failed parsing summary event", e);
                    }
                });
            },
        });

        client.activate();
        clientRef.current = client;

        return () => {
            client.deactivate();
            clientRef.current = null;
        };
    }, [tenantId, onJobEvent, onSummaryEvent]);
}
