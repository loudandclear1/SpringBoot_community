var CONTEXT_PATH = "/community";

$("form").submit(function(e){
	var pwd1 = $("#new-password").val();
	var pwd2 = $("#confirm-newPassword").val();
	if (pwd1 != pwd2) {
		$("#confirm-newPassword").addClass("is-invalid");
		return false;
	}
});

window.alert = function(message) {
	if(!$(".alert-box").length) {
		$("body").append(
			'<div class="modal alert-box" tabindex="-1" role="dialog">'+
				'<div class="modal-dialog" role="document">'+
				'<div class="modal-content">'+
					'<div class="modal-header">'+
						'<h5 class="modal-title">提示</h5>'+
						'<button type="button" class="close" data-dismiss="modal" aria-label="Close">'+
							'<span aria-hidden="true">&times;</span>'+
						'</button>'+
					'</div>'+
					'<div class="modal-body">'+
						'<p></p>'+
					'</div>'+
					'<div class="modal-footer">'+
						'<button type="button" class="btn btn-secondary" data-dismiss="modal">确定</button>'+
					'</div>'+
					'</div>'+
				'</div>'+
			'</div>'
		);
	}

    var h = $(".alert-box").height();
	var y = h / 2 - 100;
	if(h > 600) y -= 100;
    $(".alert-box .modal-dialog").css("margin", (y < 0 ? 0 : y) + "px auto");
	
	$(".alert-box .modal-body p").text(message);
	$(".alert-box").modal("show");
}

var socket = new WebSocket('ws://localhost:4000/community/ws/notify');

socket.onmessage = function(event) {
	var data = JSON.parse(event.data);
	if (data.type === 'NEW_MESSAGE') {
		addBreathingEffect();
	} else {
		removeBreathingEffect();
	}
};

function addBreathingEffect() {
	var messageLink = document.getElementById('messageLink');
	if (messageLink) {
		messageLink.classList.add('breathing');
	}
}

function removeBreathingEffect() {
	var messageLink = document.getElementById('messageLink');
	if (messageLink) {
		messageLink.classList.remove('breathing');
	}
}

