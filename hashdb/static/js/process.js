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
                  simrinwahal();  //Custom injection for link wrapping
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





// ########################################################
function simrinwahal() {
  $(document).ready(function(){
    setTimeout(function(){
      // console.log('HELLO, WORLD!');
      var ar = [];
      var ts = [];
      var a = $("#recent-result").find("td");
      for(var i = 0; i < a.length; i += 2) {  // take every second element
          ar.push(a[i]);
          // console.log(a[i].value);
          $(a[i]).html('<a class="hashLinks" href="#">'+$(a[i]).text()+'</a>')
      }
      
      //ar contains all the TD nodes with the hashes
      $('.hashLinks').on('click', function(e){
        e.preventDefault();
        var hash = $(this).text();
        modalOpen(hash);
        return;
      })


    },200);
  })
}

function modalOpen(hash, timestamp) {
  console.log('hash:', hash);
  if ($('#sim-overlay').length==0) {
    $('#wrapper').append(
      '<div id="sim-overlay"></div>'+
      '<div id="sim-modal">'+
        '<div id="cross-button"></div>'+
        '<h1 class="hash-heading">HASH</h1>'+
        '<h2 class="hash-title current_hash">'+hash+'</h2>'+
        '<h1 class="hash-heading">TIMESTAMP</h1>'+
        '<h2 class="hash-title current_timestamp">'+'timestamp to come here'+'</h2>'+
        '<h1 class="hash-heading">VISUALIZATION</h1>'+
        '<h2 class="hash-title link-proof">'+'Click here to view the proof'+'</h2>'+
      '</div>'
    );
    $('#cross-button').on('click', function(){
      console.log("here");
      modalClose();
    })

    $('.link-proof').on('click', function(){
      var hash = $('.current_hash').text();
      modalClose();
      $('#proof-toggle').click();
      $('#hashInput').val(hash);
      $('#submitHash').click();
    });

  } else {
    $('#sim-overlay, #sim-modal').css('display', 'block');
  }


  console.log("HERE!");
  $.get('/api/hashes/'+hash, function(data){
    console.log("HERE!");
    // console.log(data);
    var current_hash = data.split('\n')[0].split(':')[1].slice(1);
    var current_timestamp = data.split('\n')[1].split('added:')[1].slice(1);
    $('.current_hash').text(current_hash);
    $('.current_timestamp').text(current_timestamp);
    console.log(current_timestamp);
  })
}

function modalClose() {
  $('#sim-overlay, #sim-modal').css('display', 'none');
}