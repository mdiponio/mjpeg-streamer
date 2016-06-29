<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<title>Web Socket Test</title>
<meta charset="utf-8">
<style type="text/css">
#container {
	width: 640px;
	margin: 0 auto;
	text-align: center;
}

#port {
	border: 1px solid #000
}

#play-buttons {
	margin: 10px auto;
}
</style>
</head>
<body>
	<div id="container">
		<h1>Web Socket Streamer</h1>
		<canvas id="port" width="640" height="480"></canvas>

		<div id="play-buttons">
			<label for="is-auto">Auto: </label><input id="is-auto" type="checkbox">
			<label for="is-bin">Bin: </label><input id="is-bin" type="checkbox">
			<button id="connect-button" type="button">Connect</button>
			<input id="send-text" type="text" />
			<button id="send-button" type="button">Send</button>
			<button id="close-button" type="button">Disconnect</button>
		</div>
	</div>

	<script type="text/javascript">

        var canvas, ws, context, auto = false, bin = false;

        canvas = document.getElementById("port");
        context = canvas.getContext("2d");

        /* Event handlers. */
        document.getElementById("connect-button").onclick = function() {

            bin = document.getElementById("is-bin").checked;
            auto = document.getElementById("is-auto").checked;

            ws = new WebSocket("ws://138.25.215.71:8080/MJpegStreamer/ws?stream=coupledtanks1&timestamp=t" + (bin ? "&bin=t" : ""));
            ws.onerror = function(error) {
                console.log(error)
            };

            ws.onmessage = function(message) {
                
                createImage(message.data);
             	
                if (auto)
                {
	                 setTimeout(function() {
	                    ws.send("p");
	                }, 50);
                }
            };

            ws.onopen = function() {
                console.log("Connection opened");
                ws.send("p");
            };
        };
        
        document.getElementById("send-button").onclick = function() {
            if (ws && ws.readyState == WebSocket.OPEN)
            {
                var message = document.getElementById("send-text").value;
                console.log("Sending " + message);
                ws.send(message);
            }
        };

        document.getElementById("close-button").onclick = function() {
            if (ws && ws.readyState != WebSocket.CLOSED)
            {
                console.log("Closing connection.");
            	ws.close();
            }
        };
        
        function createImage(data) 
        {
            //createImageBitmap(data).then(function(bitmap) {
            createImageBitmap(data.slice(0, data.length, "image/jpeg")).then(function(bitmap) {
                context.drawImage(bitmap, 0, 0);
            });
        }
        
        function drawImage(data)
        {
            var img = new Image();
            img.onload = function() {
                context.drawImage(img, 0, 0);
            };
            img.width = 640;
            img.height = 480;
            img.src = bin ? URL.createObjectURL(data.slice(0, data.length, "image/jpeg")) : data;
        }

    </script>
</body>
</html>