<!DOCTYPE html>
<html lang="ru">
  <head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">  
    
    <title>DataReducer · Список ресурсов</title>
    
    <link rel="shortcut icon" href="/rapport/img/favicon.ico" />
    <link href="/rapport/css/bootstrap.min.css" rel="stylesheet">
  </head>

  <body>
    
    <header>
        <nav class="navbar navbar-light bg-light">
          <div class="container">
              <a class="navbar-brand" href="http://datareducer.ru">
                <img src="/rapport/img/logo_30x30.png">
              </a>
          </div>
        </nav>
    </header>

    <main role="main" class="container">
        <table class="table table-sm mt-3">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Наименование</th>
                    <th></th>
                    <th>Описание</th>
                </tr>
            <thead>
            <#list scripts as script>
            <#if script.webAccess>
            <tr>
                <td>${script.id}</td>
                <#assign uri = "/rapport" + script.uri>
                <td><a href="${uri}">${script.name}</a></td>
                <td>
                    <button type="button" class="btn btn-default btn-sm">XML</button>
                    <button type="button" class="btn btn-default btn-sm">JSON</button>
                </td>
                <td>${script.description}</td>
            </tr>
            </#if>
            </#list>
        </table>
    </main>

    <script src="/rapport/js/jquery-3.3.1.min.js"></script>
    <script src="/rapport/js/popper.min.js"></script>
    <script src="/rapport/js/bootstrap.min.js"></script>
    
    <script>
    $(document).ready(function(){
        $(".btn").click(function(){
                var dataType = $(this).text().toLowerCase();
                $.ajax({
                    url: $(this).closest('tr').find('a').attr('href'),
                    dataType: dataType,
                    success: function(data, status, jqXHR) {
                        window.open('data:text/' + dataType + ';charset=utf-8,' + encodeURIComponent(jqXHR.responseText));
                    },
                    error: function (error) {
                        var win = window.open();
                        win.document.open();
                        win.document.write(error.responseText);
                        win.document.close();
                    }
                });
            });
    });
    </script>
    
  </body>
</html>
