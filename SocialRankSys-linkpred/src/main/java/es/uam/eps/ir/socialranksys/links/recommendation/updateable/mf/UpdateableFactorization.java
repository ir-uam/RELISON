/* 
 * Copyright (C) 2015 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.updateable.mf;

import cern.colt.function.DoubleFunction;
import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import es.uam.eps.ir.socialranksys.links.data.updateable.index.fast.FastUpdateableItemIndex;
import es.uam.eps.ir.socialranksys.links.data.updateable.index.fast.FastUpdateableUserIndex;


/**
 * Matrix factorization.
 *
 * @author Saúl Vargas (saul.vargas@uam.es)
 *
 * @param <U> type of the users
 * @param <I> type of the items
 */
public class UpdateableFactorization<U, I> implements FastUpdateableItemIndex<I>, FastUpdateableUserIndex<U>
{

    /**
     * user matrix
     */
    protected DenseDoubleMatrix2D userMatrix;

    /**
     * item matrix
     */
    protected DenseDoubleMatrix2D itemMatrix;

    /**
     * dimensionality of the vector space
     */
    protected final int K;

    /**
     * user index
     */
    protected final FastUpdateableUserIndex<U> uIndex;

    /**
     * item index
     */
    protected final FastUpdateableItemIndex<I> iIndex;

    /**
     * Constructor.
     *
     * @param uIndex fast user index
     * @param iIndex fast item index
     * @param K dimension of the latent feature space
     * @param initFunction function to initialize the cells of the matrices
     */
    public UpdateableFactorization(FastUpdateableUserIndex<U> uIndex, FastUpdateableItemIndex<I> iIndex, int K, DoubleFunction initFunction) {
        this.userMatrix = new DenseDoubleMatrix2D(uIndex.numUsers(), K);
        this.userMatrix.assign(initFunction);
        this.itemMatrix = new DenseDoubleMatrix2D(iIndex.numItems(), K);
        this.itemMatrix.assign(initFunction);
        
        this.K = K;
        this.uIndex = uIndex;
        this.iIndex = iIndex;
    }

    /**
     * Constructor for stored factorizations.
     *
     * @param uIndex fast user index
     * @param iIndex fast item index
     * @param userMatrix user matrix
     * @param itemMatrix item matrix
     * @param K dimension of the latent feature space
     */
    public UpdateableFactorization(FastUpdateableUserIndex<U> uIndex, FastUpdateableItemIndex<I> iIndex, DenseDoubleMatrix2D userMatrix, DenseDoubleMatrix2D itemMatrix, int K) {
        this.userMatrix = userMatrix;
        this.itemMatrix = itemMatrix;
        this.K = K;
        this.uIndex = uIndex;
        this.iIndex = iIndex;
    }

    @Override
    public int numUsers() {
        return uIndex.numUsers();
    }

    @Override
    public int user2uidx(U u) {
        return uIndex.user2uidx(u);
    }

    @Override
    public U uidx2user(int uidx) {
        return uIndex.uidx2user(uidx);
    }

    @Override
    public int numItems() {
        return iIndex.numItems();
    }

    @Override
    public int item2iidx(I i) {
        return iIndex.item2iidx(i);
    }

    @Override
    public I iidx2item(int iidx) {
        return iIndex.iidx2item(iidx);
    }

    @Override
    public boolean containsUser(U u) {
        return uIndex.containsUser(u);
    }

    @Override
    public boolean containsItem(I i) {
        return iIndex.containsItem(i);
    }

    /**
     * Returns the row of the user matrix corresponding to the given user.
     *
     * @param u user
     * @return row of the user matrix
     */
    public DoubleMatrix1D getUserVector(U u) {
        int uidx = user2uidx(u);
        if (uidx < 0) {
            return null;
        } else {
            return userMatrix.viewRow(uidx);
        }
    }

    /**
     * Returns the row of the item matrix corresponding to the given item.
     *
     * @param i item
     * @return row of the item matrix
     */
    public DoubleMatrix1D getItemVector(I i) {
        int iidx = item2iidx(i);
        if (iidx < 0) {
            return null;
        } else {
            return itemMatrix.viewRow(iidx);
        }
    }

    /**
     * Returns the whole user matrix.
     *
     * @return the whole user matrix
     */
    public DenseDoubleMatrix2D getUserMatrix() {
        return userMatrix;
    }

    /**
     * Returns the whole item matrix.
     *
     * @return the whole item matrix
     */
    public DenseDoubleMatrix2D getItemMatrix() {
        return itemMatrix;
    }

    /**
     * Returns the dimension of the latent feature space.
     *
     * @return the dimension of the latent feature space
     */
    public int getK()
    {
        return K;
    }

    @Override
    public int addItem(I i)
    {
        DoubleFactory2D factory = DoubleFactory2D.dense;

        int idx = this.iIndex.addItem(i);
        if(idx == itemMatrix.rows()) // The item is definitely new
        {
            DenseDoubleMatrix2D aux = new DenseDoubleMatrix2D(1, this.getK());
            this.itemMatrix = (DenseDoubleMatrix2D) factory.appendRows(this.itemMatrix, aux);
        }
        return idx;
    }

    @Override
    public int addUser(U u)
    {
        DoubleFactory2D factory = DoubleFactory2D.dense;

        int idx = this.uIndex.addUser(u);
        if(idx == userMatrix.rows()) // The item is definitely new
        {
            DenseDoubleMatrix2D aux = new DenseDoubleMatrix2D(1, this.getK());
            this.userMatrix = (DenseDoubleMatrix2D) factory.appendRows(this.userMatrix, aux);
        }
        return idx;
    }

   /* @Override
    public int removeItem(I i)
    {
        throw new UnsupportedOperationException("Not allowed");
        int idx = this.iIndex.removeItem(i);
        DoubleFactory2D factory = DoubleFactory2D.dense;
        if(idx != 0 && idx != itemMatrix.rows())
        {
            DoubleMatrix2D aux = itemMatrix.viewPart(0, 0, idx, this.getK());
            DoubleMatrix2D aux2 = itemMatrix.viewPart(idx+1, 0, itemMatrix.rows()-idx -1, this.getK());
            this.itemMatrix = (DenseDoubleMatrix2D) factory.appendRows(aux, aux2);
        }
        else if(idx == 0)
        {
            this.itemMatrix = (DenseDoubleMatrix2D) itemMatrix.viewPart(1, 0, itemMatrix.rows()-1, this.getK());
        }
        else
        {
            this.itemMatrix = (DenseDoubleMatrix2D) itemMatrix.viewPart(0, 0, itemMatrix.rows()-1, this.getK());
        }
        return idx;
    }

    @Override
    public int removeUser(U u)
    {
        throw new UnsupportedOperationException("Not allowed");

        int idx = this.uIndex.removeUser(u);
        DoubleFactory2D factory = DoubleFactory2D.dense;
        if(idx != 0 && idx != userMatrix.rows())
        {
            DoubleMatrix2D aux = userMatrix.viewPart(0, 0, idx, this.getK());
            DoubleMatrix2D aux2 = userMatrix.viewPart(idx+1, 0, userMatrix.rows()-idx -1, this.getK());
            this.userMatrix = (DenseDoubleMatrix2D) factory.appendRows(aux, aux2);
        }
        else if(idx == 0)
        {
            this.userMatrix = (DenseDoubleMatrix2D) userMatrix.viewPart(1, 0, userMatrix.rows()-1, this.getK());
        }
        else
        {
            this.userMatrix = (DenseDoubleMatrix2D) userMatrix.viewPart(0, 0, userMatrix.rows()-1, this.getK());
        }
        return idx;
    }*/
}
