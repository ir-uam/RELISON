%
% Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
% de Madrid, http://ir.ii.uam.es.
%
%  This Source Code Form is subject to the terms of the Mozilla Public
%  License, v. 2.0. If a copy of the MPL was not distributed with this
%  file, You can obtain one at http://mozilla.org/MPL/2.0/.
%

function printtrecrec(filename,matrix,index,numUsers,graph)
% PRINTTRECREC Prints a recommendation using a TREC format
%   @args filename name of the file in which to write the recommendation
%   @args matrix matrix containing the recommendation scores
%   @args index index relating number of row/column to the corresponding user
%   @args numUsers number of users in the network
%   @args graph the current graph
%
%   @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
%   @author Pablo Castells (pablo.castells@uam.es)
%
    file = fopen(filename, 'w');
    
    [index2, vidx] = sort(index, 'descend');
        
    for i = 1:numUsers
        uIdx = index(i);
        auxscores = matrix(i, vidx);
        [scores,indices] = sort(auxscores,'descend');
        
        num = 0;
        j = 0;
        while num < 10 && j < numUsers
            j = j + 1;
            if(i == vidx(indices(j))) % Not recommend the user to himself
                continue
            elseif graph(vidx(indices(j)),i) > 0 % Not recommend reciprocals
                continue
            elseif graph(i, vidx(indices(j))) > 0 % Not recommend already existent links
            else
                num = num + 1;
                vIdx = index(vidx(indices(j)));
                fprintf(file, '%d\tQ0\t%d\t%d\t%f\tr\n',uIdx,vIdx,num,scores(j)); 
            end
        end
    end
    fclose(file);
end