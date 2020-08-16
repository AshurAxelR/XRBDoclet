window.onscroll = function() {
	upBtn = document.getElementById("upBtn");
	if(document.body.scrollTop>20 || document.documentElement.scrollTop>20)
		upBtn.style.display = "block";
	else
		upBtn.style.display = "none";
}

function scrollUp() {
	document.body.scrollTop = 0;
	document.documentElement.scrollTop = 0;
}

function toggleExt(ref) {
	var el = ref
	while((el = el.parentElement) && el.tagName=="table");
	if(el) {
		var list = el.getElementsByClassName("ext")
		var row;
		for(row of list) {
			row.classList.toggle("hide");
		}
	}
}