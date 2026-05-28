const WsClient = (() => {
    let client = null;

    function init(userId) {
        if (client?.active) return;

        client = new StompJs.Client({
            brokerURL: `ws://${location.host}/ws`,
            reconnectDelay: 5000,
            onConnect: () => {
                client.subscribe(`/topic/usuario/${userId}`, frame => {
                    const data = JSON.parse(frame.body);
                    if (data.tipo === 'notificacion') {
                        window.dispatchEvent(new CustomEvent('ef:notificacion', { detail: data }));
                    } else if (data.tipo === 'nuevo_mensaje') {
                        window.dispatchEvent(new CustomEvent('ef:nuevo_mensaje', { detail: data }));
                    } else if (data.tipo === 'solicitud_evento') {
                        window.dispatchEvent(new CustomEvent('ef:solicitud_evento', { detail: data }));
                    }
                });
            }
        });

        client.activate();
    }

    return { init };
})();
