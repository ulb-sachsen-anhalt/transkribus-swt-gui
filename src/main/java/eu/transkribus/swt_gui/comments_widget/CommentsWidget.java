package eu.transkribus.swt_gui.comments_widget;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ServerErrorException;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.client.util.SessionExpiredException;
import eu.transkribus.core.model.beans.TrpDbTag;
import eu.transkribus.core.model.beans.customtags.CommentTag;
import eu.transkribus.core.model.beans.customtags.CustomTag;
import eu.transkribus.core.model.beans.customtags.CustomTagUtil;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpLocation;
import eu.transkribus.swt.util.DialogUtil;
import eu.transkribus.swt.util.Images;
import eu.transkribus.swt.util.SWTUtil;
import eu.transkribus.swt.util.databinding.DataBinder;
import eu.transkribus.swt_gui.TrpConfig;
import eu.transkribus.swt_gui.mainwidget.TrpMainWidget;
import eu.transkribus.swt_gui.mainwidget.settings.TrpSettings;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;
import eu.transkribus.swt_gui.util.DelayedTask;

public class CommentsWidget extends Composite {
	private final static Logger logger = LoggerFactory.getLogger(CommentsWidget.class);
	
	CommentsTable commentsTable;
	Combo scopeCombo;
	Text commentText;
	Button refresh, addComment, /*saveComment,*/ deleteComment, showComments, replyComment;
//	Button scopeBtn;
	Label intro;
	
	boolean disableTagSelectionUpdate=false;

	public CommentsWidget(Composite parent, int style) {
		super(parent, style);
		this.setLayout(new FillLayout());
		
		SashForm sf = new SashForm(this, SWT.VERTICAL);
		
		Composite top = new Composite(sf, 0);
		top.setLayout(new GridLayout(3, false));
				
//		Label l = new Label(top, 0);
//		l.setText("Enter your comment:");
//		l.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 3, 1));
		
		commentText = new Text(top,  SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		commentText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
//		commentText.setMessage(String.format("Enter your comment...(Select text in editor first)")); // onyl works when SWT.SEARCH is set in Text constructor!
		
		//commentText.setText("test");
//		commentText.addModifyListener(new ModifyListener() {
//			@Override public void modifyText(ModifyEvent e) {
//				logger.debug("modifying comment...: "+commentText.getText());
//				editSelectedComment();
//			}
//		});
		
	    // add a focus listener
//	    FocusListener focusListener = new FocusListener() {
//	      public void focusGained(FocusEvent e) {
//	        Text t = (Text) e.widget;
//	        t.cut();
//	        //t.selectAll();
//	      }
//
//	      public void focusLost(FocusEvent e) {
//	        Text t = (Text) e.widget;
//	        if (t.getSelectionCount() > 0) {
//	          t.clearSelection();
//	        }
//	      }
//	    };
//	    commentText.addFocusListener(focusListener);
		commentText.addModifyListener(new ModifyListener() {
			DelayedTask dt = new DelayedTask(() -> {
				saveSelectedComment();
			}, true);
			
			@Override
			public void modifyText(ModifyEvent arg0) {
				dt.start();
			}
		});
		
		if (false) // saving on focus-lost probably not needed...
		commentText.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent arg0) {
				saveSelectedComment();
			}
			
			@Override
			public void focusGained(FocusEvent arg0) {
			}
		});

        addComment = new Button(top, SWT.PUSH);
        addComment.setImage(Images.ADD);
        addComment.setText("Add");
        addComment.setToolTipText("Adds a new comment to the selection in the transcription widget");
        addComment.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
        addComment.addSelectionListener(new SelectionAdapter() {
			@Override public void widgetSelected(SelectionEvent e) {
				addNewComment();
			}
		});
        
//        saveComment = new Button(top, SWT.PUSH);
//        saveComment.setImage(Images.DISK);
//        saveComment.setText("Save selected");
//        saveComment.setToolTipText("Save the selected comment with the text");
//        saveComment.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
//        saveComment.addSelectionListener(new SelectionAdapter() {
//			@Override public void widgetSelected(SelectionEvent e) {
//				saveSelectedComment();
//			}
//		}); 
        
        deleteComment = new Button(top, SWT.PUSH);
        deleteComment.setImage(Images.DELETE);
        deleteComment.setText("Delete");
        deleteComment.setToolTipText("Deletes the selected comment");
        deleteComment.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
        deleteComment.addSelectionListener(new SelectionAdapter() {
			@Override public void widgetSelected(SelectionEvent e) {
				deleteSelectedComment();
			}
		});  
        
        replyComment = new Button(top, SWT.PUSH);
        replyComment.setImage(Images.ARROW_UNDO);
        replyComment.setText("Reply");
        replyComment.setToolTipText("Reply to the selected comment");
        replyComment.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
        replyComment.addSelectionListener(new SelectionAdapter() {
			@Override public void widgetSelected(SelectionEvent e) {
				replySelectedComment();
			}
		});  
        
        Composite bottom = new Composite(sf, 0);
        bottom.setLayout(new GridLayout(3, false));
        
		if (false) {
		Label scopeLabel = new Label(bottom, 0);
		scopeLabel.setText("Comments - scope: ");
		
		scopeCombo = new Combo(bottom, SWT.READ_ONLY | SWT.DROP_DOWN);
//		scopeCombo.setItems(new String[] {"Page", "Document"});
		scopeCombo.setItems(new String[] {"Page"});
		scopeCombo.addSelectionListener(new SelectionAdapter() {
			@Override public void widgetSelected(SelectionEvent e) {
				reloadComments();
			}
		});
		scopeCombo.select(0);
		}
		
		refresh = new Button(bottom, SWT.PUSH);
		refresh.setImage(Images.REFRESH);
		refresh.addSelectionListener(new SelectionAdapter() {
			@Override public void widgetSelected(SelectionEvent e) {
				reloadComments();
			}
		});
		
		showComments = new Button(bottom, SWT.CHECK);
		showComments.setText("Highlight comments");
		showComments.setToolTipText("Enable special highlighting for comments in transcription widget");
		showComments.addSelectionListener(new SelectionAdapter() {
			@Override public void widgetSelected(SelectionEvent e) {
				if (TrpMainWidget.getInstance().getUi().getSelectedTranscriptionWidget()!=null)
					TrpMainWidget.getInstance().getUi().getSelectedTranscriptionWidget().redrawText(true, false, false);
			}
		});
		showComments.setSelection(TrpConfig.getTrpSettings().isHighlightComments());
		DataBinder.get().bindBeanToWidgetSelection(TrpSettings.HIGHLIGHT_COMMENTS_PROPERTY, TrpConfig.getTrpSettings(), showComments);
		
//		scopeBtn = new Button(bottom, SWT.CHECK);
//		scopeBtn.setText("Whole doc");
//		scopeBtn.setToolTipText("Show tags for the whole document");
//		SWTUtil.onSelectionEvent(scopeBtn, e -> reloadComments());
		
		Button searchTags = new Button(bottom, SWT.PUSH);
		searchTags.setText("Find all...");
		searchTags.setImage(Images.FIND);
		searchTags.setToolTipText("Find all comments on document level (can take some time...)");
		SWTUtil.onSelectionEvent(searchTags, e -> {
			TrpMainWidget.getInstance().searchTags(false, CommentTag.TAG_NAME);
		});
		
		commentsTable = new CommentsTable(bottom, 0);
		commentsTable.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		commentsTable.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override public void selectionChanged(SelectionChangedEvent event) {
				refreshSelectedComment();
			}
		});
		
		commentsTable.addDoubleClickListener(new IDoubleClickListener() {	
			@Override public void doubleClick(DoubleClickEvent event) {
				showSelectedComment();
			}
		});
		commentsTable.getTable().addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				// TODO Auto-generated method stub
				
			}
		});
		SWTUtil.onSelectionEvent(commentsTable.getTable(), e -> {
			showSelectedComment();
		});

		sf.setWeights(new int[] { 35, 65 });
	}
	
	protected void showSelectedComment() {
		CommentTag c = getSelectedComment();
		if (c==null) {
			return;
		}
		
		logger.debug("showing comment: "+c);
		disableTagSelectionUpdate = true;
		TrpMainWidget.getInstance().showLocation(new TrpLocation(c));
		disableTagSelectionUpdate = false;
	}

	private void addNewComment() {
		TrpMainWidget mw = TrpMainWidget.getInstance();
		boolean isTextSelectedInTranscriptionWidget = mw.isTextSelectedInTranscriptionWidget();
		if (!isTextSelectedInTranscriptionWidget) {
			DialogUtil.showErrorMessageBox(getShell(), "Error", "No text seleceted in transcription widget!");
			return;
		}
		
		String comment = DialogUtil.showTextInputDialog(getShell(), "Enter comment", "Please enter a comment: ", "");
		if (StringUtils.isEmpty(comment)) {
			return;
		}
		
		Map<String, Object> atts = new HashMap<>();
		atts.put(CommentTag.COMMENT_PROPERTY_NAME, comment);
		mw.addTagForSelection(CommentTag.TAG_NAME, atts, null);
		reloadComments();
	}
	
	private void saveSelectedComment() {
		logger.debug("saving selected comment...");
		CommentTag c = getSelectedComment();
		if (c != null) {
			c.setComment(commentText.getText());
			for (CustomTag ct : c.continuations) {
				if (ct instanceof CommentTag) // should always be true here
					((CommentTag)ct).setComment(commentText.getText());
			}
			
			commentsTable.refresh(c, true);
		}
	}
	
	private void deleteSelectedComment() {		
		CommentTag c = getSelectedComment();
		if (c != null) {
			c.getCustomTagList().deleteTagAndContinuations(c);
			reloadComments();
		}		
	}
	
	private void replySelectedComment() {
		CommentTag c = getSelectedComment();
		if (c==null) {
			return;
		}
		Storage store = Storage.getInstance();
		
		String username = "<unknown-user>";
		if (store !=null && store.getUserName()!=null) {
			username = store.getUserName();
		}
//		String txt = commentText.getText();
//		txt += "\n"+username+": ";
		commentText.append("\n\n");
		commentText.append(username+": ");
//		commentText.setText(txt);
		commentText.setFocus();
		commentText.setSelection(commentText.getText().length());
	}
	
	public CommentTag getSelectedComment() {
		IStructuredSelection sel = (IStructuredSelection) commentsTable.getSelection();
		if (!sel.isEmpty()) {
			return (CommentTag) sel.getFirstElement();
		}
		return null;
	}
	
	public void setSelectedComment(CustomTag customTag) {
		if (disableTagSelectionUpdate) {
			return;
		}
		
		if (customTag instanceof CommentTag) {
			commentsTable.setSelection(new StructuredSelection(customTag));
		}
	}
	
	public void refreshSelectedComment() {
		CommentTag c = getSelectedComment();
		commentText.setText(c==null ? "" : c.getComment());
	}
	
	public void reloadComments() {
		Storage store = Storage.getInstance();
		if (!store.isDocLoaded()) {
			return;
		}
		
//		if (scopeBtn.getSelection() && !store.isRemoteDoc()) {
//			DialogUtil.showErrorMessageBox(getShell(), "Error", "Listing comments all document comments is only availabe for remote docs!");
//			scopeBtn.setSelection(false);
//		}
//		boolean docScope=scopeBtn.getSelection();
		boolean docScope = false;
		if (docScope) {
			Set<Integer> collIds = new HashSet<>(Arrays.asList(store.getCollId()));
			Set<Integer> docIds = new HashSet<>(Arrays.asList(store.getDocId()));
			try {
				List<TrpDbTag> comments = store.getConnection().getSearchCalls().searchTagsLegacy(collIds, docIds, null, CommentTag.TAG_NAME, null, null, true, false, null);
				commentsTable.setInput(comments);
			} catch (SessionExpiredException | ServerErrorException | ClientErrorException e) {
				DialogUtil.showErrorMessageBox(getShell(), "Error parsing comments for document", e.getMessage());
			}
		}
		else {
			try {
				List<CommentTag> comments = CustomTagUtil.getIndexedCustomTagsForLines(store.getTranscript().getPage(), CommentTag.TAG_NAME);
				commentsTable.setInput(comments);
			}
			catch (Exception e) {
				DialogUtil.showErrorMessageBox(getShell(), "Error parsing comments for page", e.getMessage());
			}
				
//			List<CommentTag> comments = new ArrayList<>();
//			if (store.hasTranscript()) {
//				for (TrpTextRegionType r : store.getTranscript().getPage().getTextRegions(true)) {
//					for (TextLineType l : r.getTextLine()) {
//						TrpTextLineType tl = (TrpTextLineType) l;
//						List<CommentTag> cs = tl.getCustomTagList().getIndexedTags(CommentTag.TAG_NAME);
//						comments.addAll(cs);
//					}
//				}
//			}
			
		}
		commentsTable.getTable().redraw();		
	}
	
	public void updateVisibility(boolean setEnabled){
		commentText.setEnabled(setEnabled);
		refresh.setEnabled(setEnabled);
		addComment.setEnabled(setEnabled);
//		saveComment.setEnabled(setEnabled);
		deleteComment.setEnabled(setEnabled);
		showComments.setEnabled(setEnabled);
	}

	public boolean isShowComments() {
		return showComments.getSelection();
	}

}
