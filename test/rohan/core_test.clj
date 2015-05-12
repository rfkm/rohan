(ns rohan.core-test
  (:require [rohan.core :refer :all]
            [midje.sweet :refer :all]))

(tabular
 (fact "gen-route"
   (gen-route {:uri "/users" :query-string ?query} ?page) => ?ret)
 ?query           ?page ?ret
 ""               3     "/users?page=3"
 "page=2"         3     "/users?page=3"
 "foo=bar&page=1" 3     (some-checker "/users?page=3&foo=bar" "/users?foo=bar&page=3"))

(facts "pages-in-window"
  (tabular
   (fact "center"
     (pages-in-window* ?page 10 1) => ?ret)
   ?page ?ret
   5     [4 5 6]
   1     [1 2 3]
   2     [1 2 3]
   3     [2 3 4]
   4     [3 4 5]
   9     [8 9 10]
   10    [8 9 10]))

(facts "paginate"
  (fact "can get current page from query string"
    (paginate {:uri "/" :query-string "page=3"} (range 10)) => (contains {:page 3}))
  (fact "can specify current page via options map"
    (paginate {} (range 10) {:page 2}) => (contains {:page 2})
    (paginate {:uri "/" :query-string "page=3"} (range 10) {:page 2}) => (contains {:page 2})))


(facts "render-intermediate"
  (let [req {:uri "/"}]
    (fact "simple collection"
      (render-intermediate (paginate req (range 5) {:page 3 :per-page 1}))
      => [:container
          [:prev 2 "/?page=2"]
          [:page 1 "/?page=1"]
          [:page 2 "/?page=2"]
          [:page 3 "/?page=3" :active]
          [:page 4 "/?page=4"]
          [:page 5 "/?page=5"]
          [:next 4 "/?page=4"]])
    (fact "link attributes"
      (render-intermediate (paginate req (range 2) {:page 1 :per-page 1}))
      => [:container
          [:prev nil nil]
          [:page 1 "/?page=1" :active]
          [:page 2 "/?page=2"]
          [:next 2 "/?page=2"]]
      (render-intermediate (paginate req (range 2) {:page 2 :per-page 1}))
      => [:container
          [:prev 1 "/?page=1"]
          [:page 1 "/?page=1"]
          [:page 2 "/?page=2" :active]
          [:next nil nil]])

    (fact "sliding"
      (render-intermediate (paginate req (range 5) {:page 1 :per-page 1 :window-size 0}))
      => [:container
          [:prev nil nil]
          [:page 1 "/?page=1" :active]
          [:ellipsis]
          [:page 5 "/?page=5"]
          [:next 2 "/?page=2"]]

      (render-intermediate (paginate req (range 5) {:page 2 :per-page 1 :window-size 0}))
      => [:container
          [:prev 1 "/?page=1"]
          [:page 1 "/?page=1"]
          [:page 2 "/?page=2" :active]
          [:ellipsis]
          [:page 5 "/?page=5"]
          [:next 3 "/?page=3"]]

      (render-intermediate (paginate req (range 5) {:page 3 :per-page 1 :window-size 0}))
      => [:container
          [:prev 2 "/?page=2"]
          [:page 1 "/?page=1"]
          [:page 2 "/?page=2"]
          [:page 3 "/?page=3" :active]
          [:page 4 "/?page=4"]
          [:page 5 "/?page=5"]
          [:next 4 "/?page=4"]]

      (render-intermediate (paginate req (range 5) {:page 4 :per-page 1 :window-size 0}))
      => [:container
          [:prev 3 "/?page=3"]
          [:page 1 "/?page=1"]
          [:ellipsis]
          [:page 4 "/?page=4" :active]
          [:page 5 "/?page=5"]
          [:next 5 "/?page=5"]]

      (render-intermediate (paginate req (range 5) {:page 5 :per-page 1 :window-size 0}))
      => [:container
          [:prev 4 "/?page=4"]
          [:page 1 "/?page=1"]
          [:ellipsis]
          [:page 5 "/?page=5" :active]
          [:next nil nil]])))

(facts "renderer"
  (let [req {:uri "/"}]
    (facts "default renderer"
      (fact "simple collection"
        (render (paginate req (range 1 6) {:page 3 :per-page 1}))
        => [:ul {:class "pagination"}
            [:li nil [:a {:href "/?page=2"} "«"]]
            [:li nil [:a {:href "/?page=1"} "1"]]
            [:li nil [:a {:href "/?page=2"} "2"]]
            [:li {:class "active"}
             [:a {:href "/?page=3"} "3"]]
            [:li nil [:a {:href "/?page=4"} "4"]]
            [:li nil [:a {:href "/?page=5"} "5"]]
            [:li nil [:a {:href "/?page=4"} "»"]]])
      (fact "link attributes"
        (render (paginate req (range 1 3) {:page 1 :per-page 1}))
        => [:ul {:class "pagination"}
            [:li {:class "disabled"} [:span  "«"]]
            [:li {:class "active"} [:a {:href "/?page=1"} "1"]]
            [:li nil [:a {:href "/?page=2"} "2"]]
            [:li nil [:a {:href "/?page=2"} "»"]]]
        (render (paginate req (range 1 3) {:page 2 :per-page 1}))
        => [:ul {:class "pagination"}
            [:li nil [:a {:href "/?page=1"} "«"]]
            [:li nil [:a {:href "/?page=1"} "1"]]
            [:li {:class "active"} [:a {:href "/?page=2"} "2"]]
            [:li {:class "disabled"} [:span "»"]]])

      (fact "sliding"
        (render (paginate req (range 1 6) {:page 1 :per-page 1 :window-size 0}))
        => [:ul {:class "pagination"}
            [:li {:class "disabled"} [:span  "«"]]
            [:li {:class "active"} [:a {:href "/?page=1"} "1"]]
            [:li {:class "disabled"}
             [:span "…"]]
            [:li nil [:a {:href "/?page=5"} "5"]]
            [:li nil [:a {:href "/?page=2"} "»"]]]

        (render (paginate req (range 1 6) {:page 2 :per-page 1 :window-size 0}))
        => [:ul {:class "pagination"}
            [:li nil [:a {:href "/?page=1"} "«"]]
            [:li nil [:a {:href "/?page=1"} "1"]]
            [:li {:class "active"} [:a {:href "/?page=2"} "2"]]
            [:li {:class "disabled"}
             [:span "…"]]
            [:li nil [:a {:href "/?page=5"} "5"]]
            [:li nil [:a {:href "/?page=3"} "»"]]]

        (render (paginate req (range 1 6) {:page 3 :per-page 1 :window-size 0}))
        => [:ul {:class "pagination"}
            [:li nil [:a {:href "/?page=2"} "«"]]
            [:li nil [:a {:href "/?page=1"} "1"]]
            [:li nil [:a {:href "/?page=2"} "2"]]
            [:li {:class "active"} [:a {:href "/?page=3"} "3"]]
            [:li nil [:a {:href "/?page=4"} "4"]]
            [:li nil [:a {:href "/?page=5"} "5"]]
            [:li nil [:a {:href "/?page=4"} "»"]]]

        (render (paginate req (range 1 6) {:page 4 :per-page 1 :window-size 0}))
        => [:ul {:class "pagination"}
            [:li nil [:a {:href "/?page=3"} "«"]]
            [:li nil [:a {:href "/?page=1"} "1"]]
            [:li {:class "disabled"}
             [:span "…"]]
            [:li {:class "active"} [:a {:href "/?page=4"} "4"]]
            [:li nil [:a {:href "/?page=5"} "5"]]
            [:li nil [:a {:href "/?page=5"} "»"]]]

        (render (paginate req (range 1 6) {:page 5 :per-page 1 :window-size 0}))
        => [:ul {:class "pagination"}
            [:li nil [:a {:href "/?page=4"} "«"]]
            [:li nil [:a {:href "/?page=1"} "1"]]
            [:li {:class "disabled"}
             [:span "…"]]
            [:li {:class "active"} [:a {:href "/?page=5"} "5"]]
            [:li {:class "disabled"} [:span "»"]]]))))
