function pic = pseudoinversecosine(adjMatrix, numUsers)
% PSEUDOINVERSECOSINE Implementation of the pseudo inverse cosine algorithm
%   @args adjMatrix adjacency matrix
%   @args numUsers number of users in the network
%
%   @author Javier Sanz-Cruzado
%   @author Pablo Castells
%
    D = diag(sum(adjMatrix));
    L = D - adjMatrix;
    pinvL = pinv(L);
    
    pinvL2 = transpose(diag(pinvL))*diag(pinvL);
    
    pic = pinvL ./ pinvL2;
end