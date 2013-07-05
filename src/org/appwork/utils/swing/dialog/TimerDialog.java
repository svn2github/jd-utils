/**
 * Copyright (c) 2009 - 2010 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 * 
 * This file is part of org.appwork.utils.swing.dialog
 * 
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.utils.swing.dialog;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.JDialog;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import org.appwork.utils.Application;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SwingUtils;

public abstract class TimerDialog {

    public class InternDialog extends JDialog {

        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        public InternDialog() {
            super(SwingUtils.getWindowForComponent(Dialog.getInstance().getParentOwner()), ModalityType.TOOLKIT_MODAL);

            setLayout(new MigLayout("ins 5", "[]", "[fill,grow][]"));
            // JPanel contentPane;
            // setContentPane(contentPane = new JPanel());

            if (Dialog.getInstance().getIconList() != null) {
                setIconImages(Dialog.getInstance().getIconList());
            }

        }

        public TimerDialog getDialogModel() {
            return TimerDialog.this;
        }

        public void setVisible(final boolean b) {
            onSetVisible(b);
            super.setVisible(b);
        }

        @Override
        public void dispose() {
            new EDTRunner() {

                @Override
                protected void runInEDT() {
                    setDisposed(true);
                    TimerDialog.this.dispose();
                    InternDialog.super.dispose();
                }
            }.waitForEDT();

        }

        @Override
        public Dimension getPreferredSize() {
            return TimerDialog.this.getPreferredSize();

        }

        public Dimension getRawPreferredSize() {
            return super.getPreferredSize();

        }

        /**
         * 
         */
        public void realDispose() {
            super.dispose();

        }

        // @Override
        // public void setLayout(final LayoutManager manager) {
        // super.setLayout(manager);
        // }
    }

    /**
     * Timer Thread to count down the {@link #counter}
     */
    protected Thread       timer;
    /**
     * Current timer value
     */
    protected long         counter;
    /**
     * Label to display the timervalue
     */
    protected JLabel       timerLbl;

    protected InternDialog dialog;

    protected Dimension    preferredSize;
    private int            timeout           = 0;
    private boolean        countdownPausable = true;
    protected boolean      disposed          = false;

    public boolean isDisposed() {
        return disposed;
    }

    /**
     * @param b
     */
    protected void setDisposed(final boolean b) {
        disposed = b;
    }

    public boolean isCountdownPausable() {
        return countdownPausable;
    }

    public TimerDialog() {
        // super(parentframe, ModalityType.TOOLKIT_MODAL);

    }

    /**
     * @return
     */
    protected boolean isDeveloperMode() {
        // dev mode in IDE
        return !Application.isJared(AbstractDialog.class);
    }

    /**
     * @param b
     */
    public void onSetVisible(final boolean b) {
        // TODO Auto-generated method stub

    }

    /**
     * interrupts the timer countdown
     */
    public void cancel() {
        if (!isCountdownPausable()) { return; }
        if (timer != null) {
            timer.interrupt();
            timer = null;
            timerLbl.setEnabled(false);
        }
    }

    /**
     * 
     */
    protected void dispose() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                setDisposed(true);
                getDialog().realDispose();
            }
        }.waitForEDT();

    }

    /**
     * @return
     */
    protected Color getBackground() {
        // TODO Auto-generated method stub
        return getDialog().getBackground();
    }

    public void setCountdownPausable(final boolean b) {
        countdownPausable = b;

        new EDTRunner() {

            @Override
            protected void runInEDT() {
                if (timer != null && timer.isAlive()) {

                    timerLbl.setEnabled(b);
                }
            }
        };

    }

    /**
     * @return the timeout a dialog actually should display
     */
    public long getCountdown() {
        return getTimeout() > 0 ? getTimeout() : Dialog.getInstance().getDefaultTimeout();
    }

    public int getTimeout() {
        return timeout;
    }

    public InternDialog getDialog() {
        if (dialog == null) { throw new NullPointerException("Call #org.appwork.utils.swing.dialog.AbstractDialog.displayDialog() first"); }
        return dialog;
    }

    /**
     * override this if you want to set a special height
     * 
     * @return
     */
    protected int getPreferredHeight() {
        // TODO Auto-generated method stub
        return -1;
    }

    /**
     * @return
     */
    public Dimension getPreferredSize() {

        final Dimension pref = getRawPreferredSize();

        int w = getPreferredWidth();
        int h = getPreferredHeight();
        if (w <= 0) {
            w = pref.width;
        }
        if (h <= 0) {
            h = pref.height;
        }

        try {

            final Dimension ret = new Dimension(Math.min(Toolkit.getDefaultToolkit().getScreenSize().width, w), Math.min(Toolkit.getDefaultToolkit().getScreenSize().height, h));

            return ret;
        } catch (final Throwable e) {
            return pref;
        }
    }

    /**
     * @return
     */
    public Dimension getRawPreferredSize() {

        return getDialog().getRawPreferredSize();
    }

    /**
     * overwride this to set a special width
     * 
     * @return
     */
    protected int getPreferredWidth() {
        // TODO Auto-generated method stub
        return -1;
    }

    protected void initTimer(final long time) {
        counter = time / 1000;
        timer = new Thread() {

            @Override
            public void run() {
                try {
                    // sleep while dialog is invisible
                    while (!TimerDialog.this.isVisible()) {
                        try {
                            Thread.sleep(200);
                        } catch (final InterruptedException e) {
                            break;
                        }
                    }
                    long count = counter;
                    while (--count >= 0) {
                        if (!TimerDialog.this.isVisible()) {
                            //
                            return;
                        }
                        if (timer == null) {
                            //
                            return;
                        }
                        final String left = TimeFormatter.formatSeconds(count, 0);

                        new EDTHelper<Object>() {

                            @Override
                            public Object edtRun() {
                                timerLbl.setText(left);
                                return null;
                            }

                        }.start();

                        Thread.sleep(1000);

                        if (counter < 0) {
                            //
                            return;
                        }
                        if (!TimerDialog.this.isVisible()) {
                            //
                            return;
                        }

                    }
                    if (counter < 0) {
                        //
                        return;
                    }
                    if (!isInterrupted()) {
                        TimerDialog.this.onTimeout();
                    }
                } catch (final InterruptedException e) {
                    return;
                }
            }

        };

        timer.start();
    }

    /**
     * @return
     */
    protected boolean isVisible() {
        // TODO Auto-generated method stub
        return getDialog().isVisible();
    }

    protected void layoutDialog() {
        Dialog.getInstance().initLaf();

        dialog = new InternDialog();

        if (preferredSize != null) {
            dialog.setPreferredSize(preferredSize);
        }

        timerLbl = new JLabel(TimeFormatter.formatSeconds(getCountdown(), 0));
        timerLbl.setEnabled(isCountdownPausable());

    }

    protected abstract void onTimeout();

    public void pack() {

        getDialog().pack();
        if (!getDialog().isMinimumSizeSet()) {
            getDialog().setMinimumSize(getDialog().getPreferredSize());
        }

    }

    public void requestFocus() {
        getDialog().requestFocus();
    }

    protected void setAlwaysOnTop(final boolean b) {
        getDialog().setAlwaysOnTop(b);
    }

    /**
     * @deprecated use #setTimeout instead
     * @param countdownTime
     */
    @Deprecated
    public void setCountdownTime(final int countdownTimeInSeconds) {
        timeout = countdownTimeInSeconds * 1000;
    }

    /**
     * Set countdown time on Milliseconds!
     * 
     * @param countdownTimeInMs
     */
    public void setTimeout(final int countdownTimeInMs) {
        timeout = countdownTimeInMs;
    }

    protected void setDefaultCloseOperation(final int doNothingOnClose) {
        getDialog().setDefaultCloseOperation(doNothingOnClose);
    }

    protected void setMinimumSize(final Dimension dimension) {
        getDialog().setMinimumSize(dimension);
    }

    /**
     * @param dimension
     */
    public void setPreferredSize(final Dimension dimension) {
        try {
            getDialog().setPreferredSize(dimension);
        } catch (final NullPointerException e) {
            preferredSize = dimension;
        }
    }

    protected void setResizable(final boolean b) {
        getDialog().setResizable(b);
    }

    /**
     * @param b
     */
    public void setVisible(final boolean b) {
        getDialog().setVisible(b);
    }

}
