<#-- @ftlvariable name="entries" type="kotlin.collections.List
<com.jetbrains.handson.website.BlogEntry>" -->
    <!DOCTYPE html>
    <html lang="en">
    <head>
        <title>File Writer</title>
        <link rel="stylesheet" href="static/style.css">
        <script src="https://code.jquery.com/jquery-3.6.0.min.js"
                integrity="sha256-/xUj+3OJU5yExlq6GSYGSHk7tPXikynS7ogEvDej/m4="
                crossorigin="anonymous">
        </script>
        <link href="https://maxcdn.bootstrapcdn.com/font-awesome/4.2.0/css/font-awesome.min.css" rel="stylesheet">
    </head>
    <body style="font-family: sans-serif; background: #5790ff">

    <div align="middle">
        <img height="100px" src="static/tank.png"  />
        <br>
        <br>
        <div align="middle" id="placeHolder" class="card">
            <#list entries as item>
            <div id="div_${item.id}">
                <input id="input_${item.id}" value="${item.text}" class="textLine"/>
                <button id="btn_${item.id}" onclick="onClickDel(${item.id})" class="delete" >
                    Del
                </button>
                <br>
                <br>
            </div>
        </#list>
    </div>
    <br>
    <br>

    <div>
        <button class="more" onclick="addLine()">
            Add Line
        </button>
    </div>

    <div>
        <button class="more" onclick="commitChanges()">
            Commit Changes
        </button>
    </div>
    </div>


    <script>
        let allInputs = document.getElementsByTagName("input")
        let divPlaceHolder = document.getElementById("placeHolder")
        let countOfLines = allInputs.length

        function onClickDel(a){
            document.getElementById("div_"+a).remove();
        }

        function addLine(){
            newDiv = document.createElement("div")
            newInput = document.createElement("input")
            newButton = document.createElement("button")

            newDiv.setAttribute("id", "div_" + countOfLines)
            newInput.setAttribute("id", "input_" + countOfLines)
            newButton.setAttribute("id", "btn_" + countOfLines)
            newButton.setAttribute("class", "delete")
            newInput.setAttribute("class", "textLine")
            newButton.setAttribute("onclick", "onClickDel("+countOfLines+")")
            newButton.innerHTML = "Del"
            countOfLines += 1

            newDiv.appendChild(newInput)
            newDiv.appendChild(newButton)
            newDiv.appendChild(document.createElement("br"))
            newDiv.appendChild(document.createElement("br"))
            divPlaceHolder.appendChild(newDiv)
        }

        function commitChanges(){

            let resultStr = ""

            for (let i = 0; i < allInputs.length; i++) {
                let curInput = allInputs[i].value
                resultStr += curInput
                if (i != allInputs.length-1) resultStr += "\n"
            }

            $.ajax({
                url: '/commitChanges',
                cache: false,
                method: 'post',
                dataType: 'html',
                data: {text: resultStr},
                success: function(data){
                    alert(data);
                }
            });

        }




    </script>

    </body>
    </html>