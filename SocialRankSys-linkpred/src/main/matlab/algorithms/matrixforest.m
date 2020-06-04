function mf = matrixforest(adjMatrix, numUsers, alpha)
% LPIMATRIX Implementation of the matrix forest algorithm.
%   @args adjMatrix adjacency matrix
%   @args numUsers number of users in the network
%   @args alpha free parameter controlling the importance of the Laplacian matrix
%   @args k maximum distance from the target user
%
%   @author Javier Sanz-Cruzado
%   @author Pablo Castells
%
    D = diag(sum(adjMatrix));
    L = D - adjMatrix;
    X = eye(numUsers) + alpha.*L;
    
    mf = inv(X);
end