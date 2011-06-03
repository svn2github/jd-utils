package org.appwork.utils.swing.table.columns;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.Border;

import org.appwork.utils.swing.renderer.RenderLabel;
import org.appwork.utils.swing.table.ExtColumn;
import org.appwork.utils.swing.table.ExtDefaultRowSorter;
import org.appwork.utils.swing.table.ExtTableModel;

public abstract class ExtTextAreaColumn<E> extends ExtColumn<E> implements ActionListener, FocusListener {

    private static final long serialVersionUID = 2114805529462086691L;
    protected RenderLabel     renderer;
    protected JScrollPane     editor;
    private final Border      defaultBorder    = BorderFactory.createEmptyBorder(0, 5, 0, 5);
    private Color             rendererForeground;
    private Color             editorForeground;
    private Font              rendererFont;
    private Font              editorFont;
    private JTextArea         txt;
    private int               oldRowHeight;
    private int               oldRowNum;

    /**
     * @param string
     */
    public ExtTextAreaColumn(final String name) {
        this(name, null);

    }

    public ExtTextAreaColumn(final String name, final ExtTableModel<E> table) {
        super(name, table);
        this.txt = new JTextArea();
        this.editor = new JScrollPane(this.txt);

        this.txt.addFocusListener(this);
        this.renderer = new RenderLabel();
        this.rendererForeground = this.txt.getForeground();
        this.editorForeground = this.txt.getForeground();
        this.rendererFont = this.renderer.getFont();
        this.editorFont = this.txt.getFont();
        this.setRowSorter(new ExtDefaultRowSorter<E>() {

            @Override
            public int compare(final E o1, final E o2) {
                String o1s = ExtTextAreaColumn.this.getStringValue(o1);
                String o2s = ExtTextAreaColumn.this.getStringValue(o2);
                if (o1s == null) {
                    o1s = "";
                }
                if (o2s == null) {
                    o2s = "";
                }
                if (this.isSortOrderToggle()) {
                    return o1s.compareTo(o2s);
                } else {
                    return o2s.compareTo(o1s);
                }

            }

        });

    }

    public void actionPerformed(final ActionEvent e) {
        // this.txt.removeActionListener(this);
        this.fireEditingStopped();
    }

    @Override
    public void configureEditorComponent(final E value, final boolean isSelected, final int row, final int column) {
        // this.editor.removeActionListener(this);

        String str = this.getStringValue(value);
        if (str == null) {
            // under substance, setting setText(null) somehow sets the label
            // opaque.
            str = "";
        }
        this.txt.setText(str);
        final int prefSize = Math.min(120, Math.max(this.getModel().getTable().getRowHeight(row) + 20, this.txt.getPreferredSize().height));

        if (prefSize != this.getModel().getTable().getRowHeight(row)) {
            this.oldRowHeight = this.getModel().getTable().getRowHeight(row);
            this.oldRowNum = row;
            this.getModel().getTable().setRowHeight(row, prefSize);
        }
        // this.editor.addActionListener(this);

    }

    @Override
    public void configureRendererComponent(final E value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {

        String str = this.getStringValue(value);
        if (str == null) {
            // under substance, setting setText(null) somehow sets the label
            // opaque.
            str = "";
        }

        this.renderer.setText(str);
        this.renderer.setIcon(this.getIcon(value));

    }

    @Override
    public void focusGained(final FocusEvent e) {
        // this.txt.selectAll();
    }

    @Override
    public void focusLost(final FocusEvent e) {
        // TODO Auto-generated method stub
        this.stopCellEditing();
    }

    @Override
    public Object getCellEditorValue() {
        return this.txt.getText();
    }

    /**
     * @return
     */
    @Override
    public JComponent getEditorComponent(final E value, final boolean isSelected, final int row, final int column) {

        return this.editor;
    }

    /*
     * @param value
     * 
     * @return
     */
    protected Icon getIcon(final E value) {
        return null;
    }

    /**
     * @return
     */
    @Override
    public JComponent getRendererComponent(final E value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
        return this.renderer;
    }

    public abstract String getStringValue(E value);

    @Override
    protected String getToolTip(final E obj) {
        final String v = this.getStringValue(obj);
        if (v != null && v.length() > 0) {
            return "<html>" + v.replaceAll("\r\n", "<br>") + "</html>";
        } else {
            return null;
        }
    }

    @Override
    public boolean isEditable(final E obj) {
        return false;
    }

    @Override
    public boolean isEnabled(final E obj) {
        return true;
    }

    @Override
    public boolean isSortable(final E obj) {
        return true;
    }

    @Override
    public boolean matchSearch(final E object, final Pattern pattern) {
        return pattern.matcher(this.getStringValue(object)).matches();
    }

    @Override
    public void resetEditor() {
        this.editor.setFont(this.editorFont);
        this.editor.setForeground(this.editorForeground);
        this.editor.setOpaque(false);
        this.editor.setBackground(null);
    }

    @Override
    public void resetRenderer() {
        this.renderer.setBorder(this.defaultBorder);
        this.renderer.setOpaque(false);
        this.renderer.setBackground(null);
        this.renderer.setFont(this.rendererFont);
        this.renderer.setForeground(this.rendererForeground);

    }

    /**
     * Override to save value after editing
     * 
     * @param value
     * @param object
     */
    protected void setStringValue(final String value, final E object) {

    }

    // /**
    // * @param value
    // */
    // protected void prepareLabel(final E value) {
    // }

    // /**
    // * @param label2
    // */
    // protected void prepareLabelForHelpText(final JLabel label) {
    //
    // label.setForeground(Color.lightGray);
    //
    // }

    // /**
    // * Should be overwritten to prepare the component for the TableCellEditor
    // * (e.g. setting tooltips)
    // */
    // protected void prepareTableCellEditorComponent(final JTextField text) {
    // }

    // protected void prepareTextfieldForHelpText(final JTextField tf) {
    //
    // tf.setForeground(Color.lightGray);
    //
    // }

    @Override
    public void setValue(final Object value, final E object) {
        this.getModel().getTable().setRowHeight(this.oldRowNum, this.oldRowHeight);
        this.setStringValue((String) value, object);
    }

}
