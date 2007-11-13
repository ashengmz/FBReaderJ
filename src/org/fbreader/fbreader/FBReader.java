package org.fbreader.fbreader;

import org.zlibrary.core.application.ZLApplication;
import org.zlibrary.core.application.ZLKeyBindings;
import org.zlibrary.core.resources.ZLResourceKey;
import org.zlibrary.text.view.ZLTextView;

public class FBReader extends ZLApplication {
	public FBReader(String fileName) {
		super("Sample");

		addToolbarButton(ActionCode.SHOW_COLLECTION, "books");
		addToolbarButton(ActionCode.SHOW_LAST_BOOKS, "history");
		addToolbarButton(ActionCode.ADD_BOOK, "addbook");
		getToolbar().addSeparator();
		addToolbarButton(ActionCode.SCROLL_TO_HOME, "home");
		addToolbarButton(ActionCode.UNDO, "leftarrow");
		addToolbarButton(ActionCode.REDO, "rightarrow");
		getToolbar().addSeparator();
		addToolbarButton(ActionCode.SHOW_CONTENTS, "contents");
		getToolbar().addSeparator();
		addToolbarButton(ActionCode.SEARCH, "find");
		addToolbarButton(ActionCode.FIND_NEXT, "findnext");
		addToolbarButton(ActionCode.FIND_PREVIOUS, "findprev");
		getToolbar().addSeparator();
		addToolbarButton(ActionCode.SHOW_BOOK_INFO, "bookinfo");
		addToolbarButton(ActionCode.SHOW_OPTIONS, "settings");
		getToolbar().addSeparator();
		addToolbarButton(ActionCode.ROTATE_SCREEN, "rotatescreen");
		//if (ShowHelpIconOption.value()) {
			getToolbar().addSeparator();
			addToolbarButton(ActionCode.SHOW_HELP, "help");
		//}

		ZLTextView view = new ZLTextView(this, getContext());
		view.setModel(fileName);
		setView(view);
	}

	private final void addToolbarButton(ActionCode code, String name) {
		getToolbar().addButton(code.ordinal(), new ZLResourceKey(name));
	}

	public ZLKeyBindings keyBindings() {
		return null;
	}
}