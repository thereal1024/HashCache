      $("#about").hide();
      $("#search").hide();
      $("#proof").hide();
      $("#recent").hide()
      $("#about-toggle").click(function(e) {
          e.preventDefault();
          $("#about").show();
          $("#insert").hide();
          $("#search").hide();
          $("#proof").hide();
          $("#recent").hide()
      });
      $("#insert-toggle").click(function(e) {
          e.preventDefault();
          $("#insert").show();
          $("#about").hide();
          $("#search").hide();
           $("#proof").hide();
           $("#recent").hide()
      });
      $("#search-toggle").click(function(e) {
          e.preventDefault();
          $("#search").show();
          $("#about").hide();
          $("#insert").hide();
          $("#proof").hide();
          $("#recent").hide()
      });
      $("#proof-toggle").click(function(e) {
          e.preventDefault();
          $("#search").hide();
          $("#recent").hide()
          $("#proof").show();
          $("#about").hide();
          $("#insert").hide();
          $("#postHash").hide();
          $('#hashForm').show()
      });
      $("#recent-toggle").click(function(e){
          $("#search").hide();
          $("#about").hide();
          $("#insert").hide();
          $("#proof").hide();
          $("#recent").show()
          e.preventDefault();
          $.ajax({type: "GET",
                url: "api/recent/" ,
                success:function(result){

                  console.log("here")
                  var recentList = result.split("\n")
                  for(var i=0;i<recentList.length;i++){
                    var recentEntry = recentList[i].split(",")
                    var row = "<tr><td>" + recentEntry[0] + "</td><td>" + recentEntry[1] + "</td></tr>"
                    if(i==0){
                      $('#recent-result > tbody:last').html(row)   
                    }
                    else $('#recent-result > tbody:last').append(row) 
                  }
                }});
      });

      //$.get("api/window/open", function(data, status){
      //});
      var holder = document.getElementById('holder'),
          tests = {
            filereader: typeof FileReader != 'undefined',
            dnd: 'draggable' in document.createElement('span'),
            formdata: !!window.FormData,
            progress: "upload" in new XMLHttpRequest
          }, 
          support = {
            filereader: document.getElementById('filereader'),
            formdata: document.getElementById('formdata'),
            progress: document.getElementById('progress')
          },
          acceptedTypes = {
          },
          progress = document.getElementById('uploadprogress'),
          fileupload = document.getElementById('upload');
      
      "filereader formdata progress".split(' ').forEach(function (api) {
        if (tests[api] === false) {
          support[api].className = 'fail';
        } else {
          support[api].className = 'hidden';
        }
      });
      
      function previewfile(file) {
        if (tests.filereader === true && acceptedTypes[file.type] === true) {
          var reader = new FileReader();
          reader.onload = function (event) {
            var image = new Image();
            image.src = event.target.result;
            image.width = 250; // a fake resize
            holder.appendChild(image);
          };
          reader.readAsDataURL(file);
        }  else {
          holder.innerHTML += '<p>Uploaded ' + file.name;
          console.log(file);
          processInsert(file);
        }
      }
      
      function readfiles(files) {
          var formData = tests.formdata ? new FormData() : null;
          for (var i = 0; i < files.length; i++) {
            if (tests.formdata) formData.append('file', files[i]);
            previewfile(files[i]);
          }

          if (tests.formdata) {
            var xhr = new XMLHttpRequest();
            xhr.open('POST', '/devnull.php');
            xhr.onload = function() {
              progress.value = progress.innerHTML = 100;
            };
      
            if (tests.progress) {
              xhr.upload.onprogress = function (event) {
                if (event.lengthComputable) {
                  var complete = (event.loaded / event.total * 100 | 0);
                  progress.value = progress.innerHTML = complete;
                }
              }
            }
            //xhr.send(formData);
          }
      }
      
      if (tests.dnd) { 
        holder.ondragover = function () { this.className = 'hover'; return false; };
        holder.ondragend = function () { this.className = ''; return false; };
        holder.ondrop = function (e) {
          this.className = '';
          e.preventDefault();
          readfiles(e.dataTransfer.files);
        }
      } else {
        fileupload.className = 'hidden';
        fileupload.querySelector('input').onchange = function () {
          readfiles(this.files);
        };
      }
      var holder2 = document.getElementById('holder2'),
          tests2 = {
            filereader: typeof FileReader != 'undefined',
            dnd: 'draggable' in document.createElement('span'),
            formdata: !!window.FormData,
            progress: "upload" in new XMLHttpRequest
          }, 
          support2 = {
            filereader: document.getElementById('filereader2'),
            formdata: document.getElementById('formdata2'),
            progress: document.getElementById('progress2')
          },
          acceptedTypes2 = {
          },
          progress2 = document.getElementById('uploadprogress2'),
          fileupload2 = document.getElementById('upload2');
      
      "filereader formdata progress".split(' ').forEach(function (api) {
        if (tests2[api] === false) {
          support2[api].className = 'fail';
        } else {
          support2[api].className = 'hidden';
        }
      });
      
      function previewfile2(file) {
        if (tests2.filereader === true && acceptedTypes2[file.type] === true) {
          var reader = new FileReader();
          reader.onload = function (event) {
            var image = new Image();
            image.src = event.target.result;
            image.width = 250; // a fake resize
            holder2.appendChild(image);
          };
      
          reader.readAsDataURL(file);
        }  else {
          holder2.innerHTML += '<p>Uploaded ' + file.name;
          console.log(file);
          processSearch(file);
        }
      }
      
      function readfiles2(files) {
          var formData = tests2.formdata ? new FormData() : null;
          for (var i = 0; i < files.length; i++) {
            if (tests2.formdata) formData.append('file', files[i]);
            previewfile2(files[i]);
          }
      
          if (tests2.formdata) {
            var xhr = new XMLHttpRequest();
            xhr.open('POST', '/devnull.php');
            xhr.onload = function() {
              progress2.value = progress2.innerHTML = 100;
            };
      
            if (tests2.progress2) {
              xhr.upload.onprogress2 = function (event) {
                if (event.lengthComputable) {
                  var complete = (event.loaded / event.total * 100 | 0);
                  progress2.value = progress2.innerHTML = complete;
                }
              }
            }
          }
      }
      
      if (tests2.dnd) { 
        holder2.ondragover = function () { this.className = 'hover'; return false; };
        holder2.ondragend = function () { this.className = ''; return false; };
        holder2.ondrop = function (e) {
          this.className = '';
          e.preventDefault();
          readfiles2(e.dataTransfer.files);
        }
      } else {
        fileupload2.className = 'hidden';
        fileupload2.querySelector('input').onchange = function () {
          readfiles2(this.files);
        };
      }
