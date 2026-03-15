package raicod3.example.com.utilities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@NoArgsConstructor
@Getter
@Setter
public class PaginationData {

    private Object data;
    private int page;
    private int perPage;
    private long total;
    private int totalPages;

    public PaginationData(Object data, int page, int perPage, long total) {
        this.data = data;
        this.page = page;
        this.perPage = perPage;
        this.total = total;
        this.totalPages = (int) Math.ceil((double) total / perPage);
    }
}
