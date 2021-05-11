package es.uam.eps.ir.socialranksys.diffusion.sight;

import es.uam.eps.ir.socialranksys.diffusion.data.Data;
import es.uam.eps.ir.socialranksys.diffusion.data.PropagatedInformation;
import es.uam.eps.ir.socialranksys.diffusion.simulation.UserState;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Abstract implementation of a sight mechanism who values whether an information piece is observed by a user or not
 * independently.
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <P> type of the parameters.
 */
public abstract class IndividualSightMechanism<U extends Serializable,I extends Serializable,P> implements SightMechanism<U, I, P>
{
    @Override
    public void resetSelections(Data<U, I, P> data)
    {

    }

    @Override
    public List<PropagatedInformation> seesInformation(UserState<U> user, Data<U,I,P> data, List<PropagatedInformation> prop)
    {
        return prop.stream().filter(info -> this.seesInformation(user, data, info)).collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Checks if a user sees or not a piece of information.
     * @param user the user.
     * @param data the full data.
     * @param prop the information piece received by a user.
     * @return true if the user sees the piece, false if it does not.
     */
    protected abstract boolean seesInformation(UserState<U> user, Data<U, I, P> data, PropagatedInformation prop);
}