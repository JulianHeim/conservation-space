/**
 * Session countdown timer.
 */
var SessionTimer = {

	config: {
		
		// synchronization variable key name for all active session timers started in separated browser tabs
		startTimeKey: 'emf.starttime',
		
		/* Default text color of countdown timer */
		countdownColorDefault: "#000000",

		/* Level 1 warning time */
		countdownColorLevel1Mins: 0,

		/* Level 1 warning color */
		countdownColorLevel1: "#B07000",

		/* Level 2 warning time */
		countdownColorLevel2Mins: 0,

		/* Level 2 warning color */
		countdownColorLevel2: "#E00000",

		/* Count down timer element ID */
		countdownElementId: "#menuForm\\:countdownTimer",
		
		checkSessionServletUrl: '/activeSession',
		
		/* 
		 * Number of minutes before session timeout. Has to be LESS than actual timeout to avoid
		 * hidden timeout
		 */
		// count 3 seconds more to assure that the server session is really terminated 
		sessionTimeoutIntervalInSeconds: 60 //(SessionTimer.config.sessionTimeout * 60 + 3)
	},
	
	// when debug is enabled, this will hold output element in which the remaining session time will be printed
	output: null,
	
	/**
	 * Counter that starts counting down as soon as the page loads. After the countdown finishes,
	 * a function is called. It also takes care of displaying session time left on screen.
	 * To reset the timer to the initial value, countdownReset() method is to be called.
	 */
	countdownIntervalId: null,
	
	/**
	 * Method that is called right after countdown finishes.
	 * @param logout 
	 * 			If should be performed logout or not.
	 */
	sessionTimeout: function(logout) {
		SessionTimer.clearStarttime();
		document.querySelector('.logout-link').click();
//		if (logout) {
//			document.querySelector('.logout-link').click();
//		} else {
//			window.location.href = '/emf';
//		}
	},
	
	countdownReset: function() {
		clearInterval(SessionTimer.countdownIntervalId);
		SessionTimer.updateStarttime();
	},
	
	clearStarttime: function() {
		// TODO: implement fallback for not existing localStorage using a cookie
		
		// Null check for Qt based browsers that do not support localStorage.
		if (localStorage) {
			localStorage.removeItem(SessionTimer.config.startTimeKey);
		}
	},
	
	updateStarttime: function() {
		// Null check for Qt based browsers that do not support localStorage.
		if (localStorage) {
			localStorage.setItem(SessionTimer.config.startTimeKey, new Date().getTime());
		}
	},
	
	countdownInit: function() {
		SessionTimer.countdownIntervalId = setInterval( function() { SessionTimer.countdownStep(); }, 1000 );
	},
	
	countdownStep: function() {
		if (!localStorage) {
			return;
		}
		var secondsLeft = 0;
			var starttime = localStorage.getItem(SessionTimer.config.startTimeKey);
			
			// if starttime is cleared from any other tab when user clicks logout, we should end in every other tab too
			if (!starttime) {
				clearInterval(SessionTimer.countdownIntervalId);
				SessionTimer.sessionTimeout(false);
			}
			
			secondsLeft = SessionTimer.config.sessionTimeoutIntervalInSeconds - Math.round((new Date().getTime() - starttime) / 1000);
			
			if(SessionTimer.config.debugEnabled) {
				SessionTimer.output.text(secondsLeft);
			}
			
			// !!! don't make operations for visualizing - not used in the moment
			//SessionTimer.countdownDisplay(secondsLeft);
			
			if (secondsLeft <= 0) {
				// check if the session on the server is still active and 
				// if not, then redirect to the login screen
				// otherwise reset the timer and count again

				clearInterval(SessionTimer.countdownIntervalId);
				SessionTimer.sessionTimeout(true);
				
				// !!! synchronizing trough asking server resets the server session and if multiple tabs are active, then the session will never ends
//				var url = SessionTimer.config.applicationPath + SessionTimer.config.checkSessionServletUrl;
//				$.get(url)
//				.done(function(data) { 
//					if(data && data === 'false') {
//						clearInterval(SessionTimer.countdownIntervalId);
//						SessionTimer.sessionTimeout();
//					} else {
//						SessionTimer.countdownReset();
//						SessionTimer.countdownInit();
//					}
//				})
//				// if for any reason the request for checking user session fails
//				// then force logout user
//				.fail(function(data) { 
//					console.log('Error during checking if user session is active. Redirecting to login page!'); 
//					clearInterval(SessionTimer.countdownIntervalId);
//					SessionTimer.sessionTimeout();
//				});
			}
	},
	
	countdownDisplay: function(countdown) {
		var minutesLeft = 0;
		var secondsLeft = 0;
		
		minutesLeft = Math.floor(countdown / 60);
		secondsLeft = countdown % 60;
		
		var htmlElement = $(SessionTimer.config.countdownElementId);
		if (htmlElement) {
			if (minutesLeft < SessionTimer.config.countdownColorLevel2Mins) {
				htmlElement.css('color', SessionTimer.config.countdownColorLevel2);
			} else if (minutesLeft < SessionTimer.config.countdownColorLevel1Mins) {
				htmlElement.css('color', SessionTimer.config.countdownColorLevel1 );
			} else {
				htmlElement.css('color', SessionTimer.config.countdownColorDefault);
			}
			var innerHtml =  (minutesLeft < 10 ? "0" : "") + minutesLeft + ":" + (secondsLeft < 10 ? "0" : "") + secondsLeft;
			htmlElement.text(innerHtml);
		}
	},
	
	/**
	 * This is inteded to be invoked from js events such as onclick and keyup and others, in which case we assume
	 * that this plugin is already initialized and config object can be used internally as oposite to the external 
	 * configuration passed to SessionTimer.init function where we stil don't have full configuration.  
	 */
	reinit: function(evt) {
		SessionTimer.init(SessionTimer.config);
	},
	
	init: function(opts) {
		SessionTimer.config = $.extend(true, SessionTimer.config, {
			sessionTimeoutIntervalInSeconds: (opts.sessionTimeout * 60),
			countdownColorLevel1Mins: (2 * (opts.sessionTimeout / 3)),
			countdownColorLevel2Mins: (1 * (opts.sessionTimeout / 3))
		}, opts);
		
		SessionTimer.updateStarttime();
		
		if (SessionTimer.config.debugEnabled) {
			console.log('Initializing SessionTimer with configuration: ', SessionTimer.config);
			
			var output = $('#sessionTimerDebug');
			if (output.length === 0) {
				output = $('<span id="sessionTimerDebug">session timeout in:<b></b></span>').css({
					'position': 'fixed',
					'top': '80px',
					'right': '80px',
					'background-color': 'blue',
					'color': '#ffffff'
				}).appendTo(document.body);
				SessionTimer.output = output.find('b');
			}
		}
		
		$(SessionTimer.config.mainContainerId)
			.off('keyup.sessionTimer').on('keyup.sessionTimer', SessionTimer.reinit);
		// clicks should not reset timer everytime but only when real operation is done, but this is the case anyway 
//			.off('click.sessionTimer').on('click.sessionTimer', SessionTimer.reinit);
		$(document).off(SessionTimer.config.events.EMF_LOGOUT).on(SessionTimer.config.events.EMF_LOGOUT, function() {
			SessionTimer.clearStarttime();
		});
		
		SessionTimer.countdownReset();
		SessionTimer.countdownInit();
	}
};

//Register Session timer module
EMF.modules.register('SessionTimer', SessionTimer, SessionTimer.init);
