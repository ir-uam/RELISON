function printrec(filename,matrix,index,numUsers,graph)

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