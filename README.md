### For testing real-time

You could use this code snippet and run it in the browser

```html
<!doctype html>
<html>
<head>
    <meta charset="UTF-8"/>
    <title>Shipment WebSocket Test</title>
    <script src="https://cdn.jsdelivr.net/npm/sockjs-client@1.6.1/dist/sockjs.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js"></script>
</head>
<body>
<h2>Shipment Updates</h2>
<ul id="messages"></ul>

<script>
      // Connect to WebSocket endpoint
      const socket = new SockJS("http://localhost:8080/ws") // replace with your server URL if different
      const stompClient = Stomp.over(socket)

      // Optional: disable debug logs
      stompClient.debug = null

      stompClient.connect({}, function (frame) {
        console.log("Connected: " + frame)

        // Subscribe to all shipment updates
        stompClient.subscribe("/topic/shipments", function (message) {
          const msg = JSON.parse(message.body)
          console.log("Received update:", msg)

          const li = document.createElement("li")
          li.textContent = `[${msg.timestamp}] Shipment ${msg.trackingNumber} is ${msg.status}: ${msg.message}`
          document.getElementById("messages").appendChild(li)
        })

        // Example: subscribe to a specific shipment (ID = 1)
        stompClient.subscribe("/topic/shipments/1", function (message) {
          const msg = JSON.parse(message.body)
          console.log("Update for shipment 1:", msg)
        })
      })
    
</script>
</body>
</html>
```
