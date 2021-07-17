package technology.rocketjump.undermount.messaging;

/**
 * This class stores all the message types in use by MessageDispatchers (i.e. the event system)
 *
 * Does this need to be dynamic? Perhaps if scripting is introduced?
 */
public class MessageType {

	public static final int DEBUG_MESSAGE = -7;

	public static final int START_NEW_GAME = 1;
	public static final int SWITCH_SCREEN = 2;
	public static final int SWITCH_MENU = 3;
	public static final int CRASH_REPORTING_OPT_IN_MODIFIED = 4;
	public static final int CANCEL_SCREEN_OR_GO_TO_MAIN_MENU = 5;

	// Mouse & Input messages
	public static final int MOUSE_DOWN = 200;
	public static final int MOUSE_UP = 201;
	public static final int MOUSE_MOVED = 202;
	public static final int CAMERA_MOVED = 203;
	public static final int GAME_PAUSED = 204;
	public static final int MOVE_CAMERA_TO = 205;
	public static final int TRIGGER_SCREEN_SHAKE = 206;

	// GUI Messages
	public static final int ROOM_PLACEMENT = 247;
	public static final int AREA_SELECTION = 248;
	public static final int GUI_SWITCH_VIEW = 249;
	public static final int GUI_SWITCH_INTERACTION_MODE = 250;
	public static final int GUI_SWITCH_VIEW_MODE = 251;
	public static final int GUI_CANCEL_CURRENT_VIEW = 252;
	public static final int DESIGNATION_APPLIED = 253;
	public static final int REMOVE_DESIGNATION = 254;
	public static final int GUI_STOCKPILE_GROUP_SELECTED = 255;
	public static final int GUI_ROOM_TYPE_SELECTED = 256;
	public static final int GUI_FURNITURE_TYPE_SELECTED = 257;
	public static final int SET_GAME_SPEED = 258;
	public static final int GUI_SET_SCALE = 259;
	public static final int GUI_SCALE_CHANGED = 260;
	public static final int GUI_CHANGE_MUSIC_VOLUME = 261;
	public static final int GUI_CHANGE_SOUND_EFFECT_VOLUME = 262;
	public static final int GUI_CHANGE_AMBIENT_EFFECT_VOLUME = 263;
	public static final int GUI_SHOW_ERROR = 264;
	public static final int GUI_SHOW_INFO = 265;
	public static final int NOTIFY_RESTART_REQUIRED = 266;
	public static final int POST_NOTIFICATION = 267;
	public static final int SHOW_DIALOG = 268;
	public static final int REMOTE_VERSION_FOUND = 269;
	public static final int CHOOSE_SELECTABLE = 270;
	public static final int TOOLTIP_AREA_ENTERED = 271;
	public static final int TOOLTIP_AREA_EXITED = 272;
	public static final int CLEAR_ALL_TOOLTIPS = 273;
	public static final int HINT_ACTION_TRIGGERED = 274;
	public static final int REPLACE_JOB_PRIORITY = 275;
	public static final int SHOW_SPECIFIC_CRAFTING = 278;
	public static final int SET_MAIN_MENU_BACKGROUND_SCROLLING = 279;
	public static final int PREFERENCE_CHANGED = 280;
	public static final int GUI_CANCEL_CURRENT_VIEW_OR_GO_TO_MAIN_MENU = 281;

	// i18n Messages
	public static final int LANGUAGE_CHANGED = 290;
	public static final int LANGUAGE_TRANSLATION_INCOMPLETE = 291;

	// Entity messages
	public static final int ENTITY_POSITION_CHANGED = 310;
	public static final int PLANT_SEED_DISPERSED = 321;
	public static final int REQUEST_PLANT_REMOVAL = 322;
	public static final int DESTROY_ENTITY = 323;
	public static final int TREE_FELLED = 324;
	public static final int ENTITY_ASSET_UPDATE_REQUIRED = 325;
	public static final int ENTITY_CREATED = 326;
	public static final int ENTITY_DO_NOT_TRACK = 327;
	public static final int ITEM_PRIMARY_MATERIAL_CHANGED = 328;

	public static final int ENTITY_FELL_ASLEEP = 330;
	public static final int ENTITY_WOKE_UP = 331;

	public static final int PATHFINDING_REQUEST = 332;
	public static final int CHANGE_PROFESSION = 333;

	public static final int APPLY_STATUS = 334;
	public static final int REMOVE_STATUS = 335;

	public static final int TRANSFORM_FURNITURE_TYPE = 336;
	public static final int TRANSFORM_ITEM_TYPE = 337;

	public static final int ITEM_CREATION_REQUEST = 338;
	public static final int PLANT_CREATION_REQUEST = 339;
	public static final int HUMANOID_DEATH = 340;
	public static final int HUMANOID_INSANITY = 341;
	public static final int LIQUID_AMOUNT_CHANGED = 342;
	public static final int TREE_SHED_LEAVES = 343;

	public static final int FURNITURE_IN_USE = 345;
	public static final int FURNITURE_NO_LONGER_IN_USE = 346;

	// Assets and modding related messages
	public static final int SHUTDOWN_IN_PROGRESS = 400;

	// Game clock and environment messages
	public static final int DAY_ELAPSED = 500;
	public static final int SEASON_ELAPSED = 501;
	public static final int YEAR_ELAPSED = 502;

	// Job messages
	public static final int JOB_REQUESTED = 600;
	public static final int JOB_ASSIGNMENT_CANCELLED = 602;
	public static final int JOB_ASSIGNMENT_ACCEPTED = 603;
	public static final int JOB_COMPLETED = 604;
	public static final int ADD_WALL = 605;
	public static final int WALL_CREATED = 606;
	public static final int REMOVE_WALL = 607;
	public static final int WALL_REMOVED = 608;
	public static final int JOB_REMOVED = 609; // As the job is actually removed from the game world
	public static final int JOB_CREATED = 610;
	public static final int JOB_CANCELLED = 611; // When the job is longer wanted
	public static final int CHANGE_FLOOR = 612;
	public static final int REMOVE_HAULING_JOBS_TO_POSITION = 613;
	public static final int JOB_STATE_CHANGE = 614;
	public static final int REQUEST_DUMP_LIQUID_CONTENTS = 615;

	// Item-specific messages
	public static final int HAULING_ALLOCATION_CANCELLED = 700;
	public static final int REQUEST_ITEM_HAULING = 702;
	public static final int REQUEST_HAULING_ALLOCATION = 703;
	public static final int LOOKUP_ITEM_TYPE = 704;
	public static final int SELECT_AVAILABLE_MATERIAL_FOR_ITEM_TYPE = 705;

	// Furniture & doorway specific messages
	public static final int CREATE_DOORWAY = 801;
	public static final int FURNITURE_MATERIAL_SELECTED = 802;
	public static final int ROTATE_FURNITURE = 803;
	public static final int DOOR_OPENED_OR_CLOSED = 804;
	public static final int FURNITURE_PLACEMENT = 805;
	public static final int DOOR_PLACEMENT = 806;
	public static final int REQUEST_DOOR_OPEN = 807;
	public static final int DECONSTRUCT_DOOR = 808;
	public static final int REMOVE_ROOM = 809;
	public static final int REMOVE_ROOM_TILES = 810;
	public static final int WALL_MATERIAL_SELECTED = 811;
	public static final int WALL_PLACEMENT_SELECTED = 812;
	public static final int WALL_PLACEMENT = 813;
	public static final int BRIDGE_MATERIAL_SELECTED = 814;
	public static final int REQUEST_FURNITURE_REMOVAL = 815;
	public static final int DOOR_MATERIAL_SELECTED = 816;
	public static final int REQUEST_FURNITURE_ASSIGNMENT = 817;
	public static final int ROOF_SUPPORT_REMOVED = 818;
	public static final int ROOF_COLLAPSE = 819;
	public static final int ROOF_TILE_COLLAPSE = 820;
	public static final int FLOOR_MATERIAL_SELECTED = 821;

	// Construction-specific messages
	public static final int CANCEL_CONSTRUCTION = 900;
	public static final int CONSTRUCTION_COMPLETED = 901;
	public static final int CONSTRUCTION_REMOVED = 902;
	public static final int TRANSFORM_CONSTRUCTION = 903;
	public static final int BRIDGE_PLACEMENT = 904;
	public static final int REQUEST_BRIDGE_REMOVAL = 905;
	public static final int DECONSTRUCT_BRIDGE = 906;
	public static final int CONSTRUCTION_PRIORITY_CHANGED = 907;
	public static final int ROOF_CONSTRUCTION_QUEUE_CHANGE = 908;
	public static final int ROOF_DECONSTRUCTION_QUEUE_CHANGE = 909;
	public static final int ROOF_CONSTRUCTED = 910;
	public static final int ROOF_DECONSTRUCTED = 911;
	public static final int FLOORING_CONSTRUCTED = 912;

	// Production messages
	public static final int REQUEST_PRODUCTION_ASSIGNMENT = 1000;
	public static final int PRODUCTION_ASSIGNMENT_ACCEPTED = 1001;
	public static final int PRODUCTION_ASSIGNMENT_CANCELLED = 1002;
	public static final int PRODUCTION_ASSIGNMENT_COMPLETED = 1003;

	// Food-related messages
	public static final int REQUEST_LIQUID_TRANSFER = 1100;
	public static final int COOKING_COMPLETE = 1101;
	public static final int FOOD_ALLOCATION_REQUESTED = 1102;
	public static final int FOOD_ALLOCATION_CANCELLED = 1103;
	public static final int LIQUID_ALLOCATION_CANCELLED = 1104;
	public static final int REQUEST_LIQUID_ALLOCATION = 1105;
	public static final int REQUEST_LIQUID_REMOVAL = 1106;
	public static final int LIQUID_SPLASH = 1107;

	// Saved game messages
	public static final int REQUEST_SAVE = 1200;
	public static final int PERFORM_LOAD = 1201;
	public static final int PERFORM_SAVE = 1202;
	public static final int TRIGGER_QUICKLOAD = 1203;
	public static final int SHOW_AUTOSAVE_PROMPT = 1204;
	public static final int HIDE_AUTOSAVE_PROMPT = 1205;
	public static final int SAVE_COMPLETED = 1206;
	public static final int SAVED_GAMES_PARSED = 1207;
	public static final int SAVED_GAMES_LIST_UPDATED = 1208;

	// Map messages
	public static final int FLOOD_FILL_EXPLORATION = 1300;
	public static final int SHOW_MINIMAP = 1301;
	public static final int REPLACE_REGION = 1302;
	public static final int AMBIENCE_UPDATE = 1303;
	public static final int AMBIENCE_PAUSE = 1304;

	// Sound/audio messages
	public static final int REQUEST_SOUND = 1400;
	public static final int REQUEST_SOUND_ASSET = 1401;
	public static final int REQUEST_STOP_SOUND_LOOP = 1402;

	// Twitch integration messages
	public static final int TWITCH_AUTH_CODE_SUPPLIED = 1500;
	public static final int TWITCH_AUTH_CODE_FAILURE = 1501;
	public static final int TWITCH_TOKEN_UPDATED = 1502;
	public static final int TWITCH_ACCOUNT_INFO_UPDATED = 1503;

	// Particle effect messages
	public static final int PARTICLE_REQUEST = 1600;
	public static final int PARTICLE_RELEASE = 1602; // Used to set particles to expire
	public static final int GET_PROGRESS_BAR_EFFECT_TYPE = 1603;

	// Weather and fire messages
	public static final int SPREAD_FIRE = 1700;


}
