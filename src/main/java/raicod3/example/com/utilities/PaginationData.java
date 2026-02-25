package raicod3.example.com.utilities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@NoArgsConstructor
@Getter
@Setter
public class PaginationData {

    private Object items;
    private int page;
    private int perPage;
    private int total;
    private int totalPages;

    public PaginationData(Object items, int page, int perPage, int total) {
        this.items = items;
        this.page = page;
        this.perPage = perPage;
        this.total = total;
        this.totalPages = (int) Math.ceil((double) total / perPage);
    }
}
