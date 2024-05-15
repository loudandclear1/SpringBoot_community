$(function(){
    $("#topBtn").click(setTop);
    $("#wonderfulBtn").click(setWonderful);
    $("#deleteBtn").click(setDelete);
});

var header = $("meta[name='_csrf_header']").attr("content"); // 例如 "X-CSRF-TOKEN"
var token = $("meta[name='_csrf']").attr("content"); // CSRF 令牌的值

function like(btn, entityType, entityId, entityUserId, postId) {
    $.ajax({
        type: "POST",
        url: CONTEXT_PATH + "/like",
        data: {
            "entityType": entityType,
            "entityId": entityId,
            "entityUserId": entityUserId,
            "postId": postId
        },
        headers: {
            [header]: token  // 直接在请求头中设置 CSRF Token
        },
        success: function (data) {
            data = $.parseJSON(data);
            if (data.code == 0) {
                $(btn).children("i").text(data.likeCount);
                $(btn).children("b").text(data.likeStatus == 1 ? '已赞' : "赞");
            } else {
                alert(data.msg);
            }
        },
        error: function () {
            alert("请求失败！");
        }
    });
}

// 置顶
function setTop() {
    $.ajax({
        type: "POST",
        url: CONTEXT_PATH + "/discuss/top",
        data: {"id": $("#postId").val()},
        headers: {
            [header]: token
        },
        success: function(data) {
            data = $.parseJSON(data);
            if (data.code == 0) {
                window.location.reload();
            } else {
                alert(data.msg);
            }
        },
        error: function() {
            alert("请求失败！");
        }
    });
}

// 加精
function setWonderful() {
    $.ajax({
        type: "POST",
        url: CONTEXT_PATH + "/discuss/wonderful",
        data: {"id": $("#postId").val()},
        headers: {
            [header]: token
        },
        success: function(data) {
            data = $.parseJSON(data);
            if (data.code == 0) {
                window.location.reload();
            } else {
                alert(data.msg);
            }
        },
        error: function() {
            alert("请求失败！");
        }
    });
}

// 删除
function setDelete() {

    $.ajax({
        type: "POST",
        url: CONTEXT_PATH + "/discuss/delete",
        data: {"id": $("#postId").val()},
        headers: {
            [header]: token
        },
        success: function(data) {
            data = $.parseJSON(data);
            if (data.code == 0) {
                location.href = CONTEXT_PATH + "/index";
            } else {
                alert(data.msg);
            }
        },
        error: function() {
            alert("请求失败！");
        }
    });
}