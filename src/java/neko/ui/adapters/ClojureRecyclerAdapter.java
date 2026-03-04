/*
 * Copyright © 2024 Alexander Yakushev
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at
 * <http://www.eclipse.org/legal/epl-v10.html>.
 *
 * By using this software in any fashion, you are agreeing to be bound by the
 * terms of this license.  You must not remove this notice, or any other, from
 * this software.
 */
package neko.ui.adapters;

import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import clojure.lang.IFn;
import java.util.Collections;
import java.util.List;

/**
 * A RecyclerView.Adapter that delegates view creation and binding to Clojure
 * functions, following the same IFn-based pattern as InterchangeableListAdapter.
 *
 * <ul>
 *   <li>{@code createViewFn}: {@code (fn [parent view-type] -> View)}</li>
 *   <li>{@code bindViewFn}: {@code (fn [view position data-item] -> void)}</li>
 *   <li>{@code itemIdFn} (optional): {@code (fn [position data-item] -> long)}</li>
 * </ul>
 */
public class ClojureRecyclerAdapter extends RecyclerView.Adapter<ClojureRecyclerAdapter.ViewHolder> {

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View itemView) {
            super(itemView);
        }
    }

    private final IFn createViewFn;
    private final IFn bindViewFn;
    private IFn itemIdFn;
    private List data;

    public ClojureRecyclerAdapter(IFn createViewFn, IFn bindViewFn) {
        this(createViewFn, bindViewFn, Collections.emptyList());
    }

    public ClojureRecyclerAdapter(IFn createViewFn, IFn bindViewFn, List initialData) {
        super();
        this.createViewFn = createViewFn;
        this.bindViewFn = bindViewFn;
        this.data = initialData;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = (View) createViewFn.invoke(parent, viewType);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        bindViewFn.invoke(holder.itemView, position, data.get(position));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @Override
    public long getItemId(int position) {
        if (itemIdFn != null) {
            return (Long) itemIdFn.invoke(position, data.get(position));
        }
        return position;
    }

    public void setItemIdFn(IFn fn) {
        this.itemIdFn = fn;
        setHasStableIds(fn != null);
    }

    public void setData(List newData) {
        this.data = newData;
        notifyDataSetChanged();
    }

    public Object getItem(int position) {
        return data.get(position);
    }
}
