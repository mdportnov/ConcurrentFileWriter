<!DOCTYPE html>
<html lang="en">
<head>
    <title>File Writer</title>
    <link rel="stylesheet" href="static/style.css">
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"
            integrity="sha256-/xUj+3OJU5yExlq6GSYGSHk7tPXikynS7ogEvDej/m4="
            crossorigin="anonymous">
    </script>
</head>
<body style="font-family: sans-serif; background: #c0d3ff">

<div align="middle">
    <h1 class="userNameField"></h1>
    <button class="more" onclick="sendStatusOpenConnection()">Connect</button>
    <button class="more" onclick="sendStatusCloseConnection()">Disconnect</button>
    <br>
    <div align="middle" id="placeHolder" class="card">
        <#list entries as item>
            <div id="div_${item.id}">
                <input id="input_${item.id}" value="${item.text}" class="textLine"/>
                <button id="btn_${item.id}" onclick="onClickDelete(${item.id})" class="delete">
                    Delete
                </button>
                <br>
            </div>
        </#list>
    </div>
    <br>

    <button class="more" onclick="addLine()">
        Add new line
    </button>

    <button class="more" onclick="commitChanges()">
        Commit Changes
    </button>

    <div class="logs"></div>
</div>

<script>
    let allInputs = document.getElementsByTagName("input")
    let divPlaceHolder = document.getElementById("placeHolder")
    let countOfLines = allInputs.length

    let userName = document.getElementsByClassName("userNameField")

    let socket = new WebSocket("ws://localhost:8080/ws");

    // $(window).unload(function () {
    //     socket.close();
    // });

    socket.onmessage = function (event) {
        console.log(event.data)
        if (event.data.toString().startsWith("You've logged in as")) {
            userName.item(0).innerHTML = event.data
        }
        if (event.data.toString().startsWith("Received data:")) {
            userName.item(0).innerHTML = event.data
        }
        if (event.data.toString().endsWith("Mutex is locked.")) {
            onClickDelete(countOfLines - 1)
        }
        // if (event.data.toString().endsWith("Update Event")) {
        //
        // }

        const node = document.createElement('ul');
        node.appendChild(document.createTextNode(event.data));
        document.getElementsByClassName("logs").item(0)
            .appendChild(node)
    }

    function sendStatusOpenConnection() {
        socket.send("OpenConnection")
    }

    function sendStatusCloseConnection() {
        socket.send("CloseConnection")
    }

    function onClickDelete(n) {
        console.log("Delete element" + n)
        document.getElementById("div_" + n).remove();
    }

    function addLine() {
        let newDiv = document.createElement("div");
        let newInput = document.createElement("input")
        let newButton = document.createElement("button")

        newDiv.setAttribute("id", "div_" + countOfLines)
        newInput.setAttribute("id", "input_" + countOfLines)
        newButton.setAttribute("id", "btn_" + countOfLines)
        newButton.setAttribute("class", "delete")
        newInput.setAttribute("class", "textLine")
        newButton.setAttribute("onclick", "onClickDelete(" + countOfLines + ")")
        newButton.innerHTML = "Delete"
        countOfLines += 1
        newDiv.appendChild(newInput)
        newDiv.appendChild(newButton)
        divPlaceHolder.appendChild(newDiv)
    }

    function commitChanges() {
        let resultStr = ""

        for (let i = 0; i < allInputs.length; i++) {
            let curInput = allInputs[i].value
            resultStr += curInput
            if (i !== allInputs.length - 1) resultStr += "\n"
        }
        console.log("Result:" + resultStr)
        socket.send(resultStr)
    }
</script>

</body>
</html>