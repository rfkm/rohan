(ns rohan.core
  (:require [clojure.string :as str]
            [clojure.walk :as walk]
            [ring.util.codec :as codec]
            [rohan.util :as u]))

(defprotocol Pageable
  (count-all [this])
  (fetch-entries [this page per-page]))

(defmacro ^:private define-default-pageable [target]
  `(extend-protocol Pageable
     (class ~target)
     (count-all [this#]
       (count this#))
     (fetch-entries [this# page# per-page#]
       (if (pos? page#)
         (take per-page# (drop (* (dec page#) per-page#) this#))
         (empty this#)))))

(extend-protocol Pageable
  Object
  (count-all [this]
    (if (u/seqable? this)
      (do (define-default-pageable this)
          (count-all this))
      (throw (ex-info "Unhandled entity" {:entity this}))))
  (fetch-entries [this page per-page]
    (if (u/seqable? this)
      (do (define-default-pageable this)
          (fetch-entries this page per-page))
      (throw (ex-info "Unhandled entity" {:entity this})))))

(defn form-decode-map [params]
  (let [params (codec/form-decode (or params ""))]
    (if (map? params) params {})))

(defn gen-route
  ([req page]
   (gen-route req page "page"))
  ([req page query-key]
   (when page
     (let [query-string (-> req
                            :query-string
                            form-decode-map
                            (->> (mapcat vec)
                                 (apply sorted-map))
                            (assoc query-key page)
                            codec/form-encode)]
       (str (:uri req) "?" query-string)))))

(defn get-current-page-from-request
  ([req]
   (get-current-page-from-request req "page"))
  ([req query-key]
   (try (-> req
            :query-string
            form-decode-map
            (get query-key)
            (Integer/parseInt))
        (catch NumberFormatException e nil))))

(defn page [pagination]
  (get pagination :page))

(defn per-page [pagination]
  (get pagination :per-page))

(defn total-entries [pagination]
  @(get pagination :total-entries))

(defn entries [pagination]
  @(get pagination :entries))

(defn entry-range [pagination]
  (if (empty? (entries pagination))
    [nil nil]
    (let [limit (per-page pagination)
          total (total-entries pagination)
          st (inc (* (dec (page pagination))
                     limit))
          ed (min (dec (+ st limit))
                  total)]
      [st ed])))

(defn window-size [pagination]
  (get pagination :window-size))

(defn total-pages* [pagination]
  (int (Math/ceil (/ (total-entries pagination) (per-page pagination)))))

(defn total-pages [pagination]
  @(get pagination :total-pages))

(defn next-page [pagination]
  (let [page (page pagination)]
    (when (< page (total-pages pagination)) (inc page))))

(defn previous-page [pagination]
  (let [page (page pagination)]
    (when (> page 1) (dec page))))

(defn pages-in-window*
  ([pagination]
   (pages-in-window* (page pagination)
                     (total-pages pagination)
                     (window-size pagination)))
  ([page last-page window-size]
   (let [st     (- page window-size)
         ed     (+ page window-size)
         offset (max 0 (- 1 st))
         st     (max 1 st)
         ed     (+ ed offset)
         offset (max 0 (- ed last-page))
         ed     (min last-page ed)
         st     (max 1 (- st offset))]
     (range st (inc ed)))))

(defn pages-in-window [pagination]
  @(get pagination :pages-in-window))

(defn paginate [req pageable & [{:keys [page per-page window-size theme]}]]
  (let [page        (or page (get-current-page-from-request req) 1)
        per-page    (or per-page 20)
        window-size (or window-size 2)
        pagination  {:page            page
                     :window-size     window-size
                     :per-page        per-page
                     :total-entries   (delay (count-all pageable))
                     :entries         (delay (fetch-entries pageable page per-page))
                     :route-generator (partial gen-route req)
                     :theme           :bootstrap3}]
    (-> pagination
        (#(assoc % :total-pages (delay (total-pages* %))))
        (#(assoc % :pages-in-window (delay (pages-in-window* %)))))))


(defn render-intermediate [pagination & opt]
  (let [current              (page pagination)
        pages                (pages-in-window pagination)
        start-page-in-window (first pages)
        last-page-in-window  (last pages)
        last-page            (total-pages pagination)
        route                (:route-generator pagination)
        prev                 (previous-page pagination)
        next                 (next-page pagination)]
    (remove nil?
            `[:container
              ~[:prev prev (when prev (route prev))]

              ~@(when (> start-page-in-window 1)
                  [[:page 1 (route 1)]
                   (cond
                     (= start-page-in-window 3)    [:page 2 (route 2)]
                     (not= start-page-in-window 2) [:ellipsis])])

              ~@(for [page pages
                      :let [link (route page)]]
                  (if (= page current)
                    [:page page link :active]
                    [:page page link]))

              ~@(when-not (= last-page last-page-in-window)
                  [(cond
                     (= last-page-in-window (- last-page 2))    [:page (dec last-page) (route (dec last-page))]
                     (not= last-page-in-window (dec last-page)) [:ellipsis])
                   [:page last-page (route last-page)]])

              ~[:next next (when next (route next))]])))

(defmulti render (fn [pagination & opt] (:theme pagination)))

(defmulti render-pager-element (fn [theme args] [theme (first args)]))

(defmethod render :default [pagination & opt]
  (let [intermediate (render-intermediate pagination)]
    (render-pager-element (:theme pagination) intermediate)))

(defmethod render-pager-element [:bootstrap3 :container] [theme [_ & elements]]
  `[:ul {:class "pagination"}
    ~@(mapv #(render-pager-element theme %) elements)])

(defn pager-element [content href & {:keys [disabled active]}]
  (let [cl (remove nil? [(when disabled "disabled")
                         (when active "active")])]
    [:li (when (seq cl)
           {:class (str/join " " cl)})
     (if (and href (not disabled))
       [:a {:href href} (str content)]
       [:span  (str content)])]))

(defmethod render-pager-element [:bootstrap3 :prev] [_ [_ _ link]]
  (pager-element "«" link :disabled (nil? link)))

(defmethod render-pager-element [:bootstrap3 :next] [_ [_ _ link]]
  (pager-element "»" link :disabled (nil? link)))

(defmethod render-pager-element [:bootstrap3 :ellipsis] [_ _]
  (pager-element "…" nil :disabled true))

(defmethod render-pager-element [:bootstrap3 :page] [_ [_ page link & attrs]]
  (let [create-attr-pair (fn [attr] [attr (.contains (vec attrs) attr)]) ; e.g., => [:disabled true]
        possible-attrs [:active :disabled]]
    (->> possible-attrs
         (mapv create-attr-pair)
         flatten
         (apply pager-element page link))))
