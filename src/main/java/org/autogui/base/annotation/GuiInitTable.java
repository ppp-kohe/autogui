package org.autogui.base.annotation;

/**
 * initial settings for table : determines the initial row-height for the table of the attached type
 * <pre>
 *     &#64;GuiIncluded
 *     &#64;GuiInits(table = &#64;{@link GuiInitTable}(rowFitToContent=true))
 *     public List&lt;MyElem&gt; getMyTable() { //the created table reflects the above setting of the annotation
 *         ...
 *     }
 * </pre>
 * @see org.autogui.GuiInits
 * @since 1.8
 */
public @interface GuiInitTable {
    /**
     * @return if true (and {@link #rowHeight()}==0), enables the row height fits to contents.  the default is false: disabled
     */
    boolean rowFitToContent() default false;

    /**
     * @return if >0, sets the row height to the value.  the default is 0: disabled.
     */
    int rowHeight() default 0;

    /**
     * @return if true, enables auto-resizing columns for the table that has a dynamic column.
     *   the default is false.
     *  Note: always auto-resizing if the table only has static columns
     */
    boolean dynamicColumnAutoResize() default false;
}
