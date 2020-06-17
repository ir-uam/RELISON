%
% Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
% de Madrid, http://ir.ii.uam.es.
%
%  This Source Code Form is subject to the terms of the Mozilla Public
%  License, v. 2.0. If a copy of the MPL was not distributed with this
%  file, You can obtain one at http://mozilla.org/MPL/2.0/.
%


function ht = hittingTimeMatrix(T, numUsers)
% HITTINGTIMEMATRIX Finds the hitting time from a Markov chain transition matrix
%   @args T transition matrix
%   @args numUsers number of users in the network
%
%   @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
%   @author Pablo Castells (pablo.castells@uam.es)
%
    I = eye(numUsers);
    A = I - T;
    U = A(1:numUsers-1,1:numUsers-1);
    d = A(numUsers,1:numUsers-1);

    invU = inv(U);
    h = d/U;
    beta = 1 - sum(h);

    w = zeros(1,numUsers);
    w(1:numUsers-1) = -h(1:numUsers-1)/beta;
    w(numUsers) = 1/beta;
    W = repmat(w,numUsers,1);

    aux = zeros(numUsers, numUsers);
    aux(1:numUsers-1,1:numUsers-1) = invU;
    AA = I - W;
    AHash = AA*aux*AA;
    AHashDiag = diag(diag(AHash));

    J = ones(numUsers, numUsers);
    aux = I - AHash + J*AHashDiag;
    pi = zeros(numUsers, numUsers);
    for i = 1:numUsers
        pi(i,i) = 1./w(i);
    end

    ht = aux*pi;
end