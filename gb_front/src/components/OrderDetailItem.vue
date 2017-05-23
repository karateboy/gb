<template>
    <div class="modal inmodal" tabindex="-1" role="dialog" aria-hidden="true">
        <div class="modal-dialog modal-lg">
            <div class="modal-content animated fadeIn">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal"><span
                            aria-hidden="true">&times;</span><span class="sr-only">Close</span></button>
                    <h4 class="modal-title">新增細項</h4>
                </div>
                <div class="modal-body">
                    <form>
                        <div class="form-group">
                            <label class="col-lg-2 control-label">顏色:</label>
                            <div class="col-lg-2"><input type="text" class="form-control"
                                                         v-model="detail.color"></div>
                        </div>
                        <div class="form-group">
                            <label class="col-lg-2 control-label">尺寸:</label>
                            <div class="col-lg-2"><input type="text" class="form-control"
                                                         v-model="detail.size"></div>
                            <div class="col-lg-8">
                                <div class="btn-group" data-toggle="buttons">
                                    <label class="btn btn-outline btn-primary"
                                           v-for="sizeOpt in sizeList"
                                           @click="detail.size=sizeOpt">
                                        <input type="radio">{{ sizeOpt }} </label>
                                </div>
                            </div>
                        </div>
                        <div class="form-group">
                            <label class="col-lg-2 control-label">數量(打):</label>
                            <div class="col-lg-2"><input type="text" class="form-control"
                                                         v-model="dozenNumber"></div>
                        </div>
                    </form>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-white" data-dismiss="modal">取消</button>
                    <button type="button" class="btn btn-primary" data-dismiss="modal" @click="addDetail" v-if='opType=="add"'>
                        新增
                    </button>
                    <button type="button" class="btn btn-primary" data-dismiss="modal" @click="updateDetail" v-else>
                        更新
                    </button>
                </div>
            </div>
        </div>
    </div>
</template>
<style>
    body{
        background-color:#ff0000;
    }


</style>
<script>
    import * as dozenExp from '../dozenExp'
    export default{
        props: {
            opType: {
                type: String,
                required: true
            },
            detailIndex: {
                type: Number
            },
            detail: {
                type: Object,
                required: true
            }
        },
        data(){
            return {
                sizeList: [
                    'XS/S', 'S/M', 'M/L', 'L/XL', '2-6', '8-12', 'XXS', 'XS', 'SS', 'S', 'M', 'L', 'XL', 'XXL', 'M/S',
                    'T', 'A/B', 'C/D', 'A', 'B', 'C', 'D', 'E',
                    'XSml', 'Sml', 'Med', 'Lge', 'Xlge', 'Sml/Med', 'Lge/Xlge', 'ChSml', 'ChLge', 'Adult', 'ChSml/ChMed',
                    'ChMed/ChLge', 'ChLge/ChXLge',
                    '0-6', '6-12', '12-24', '2T3T', '4T5T', '6-8'
                ]
            }
        },
        computed:{
            dozenNumber:{
                get: function(){
                    return dozenExp.toDozenStr(this.detail.quantity)
                },
                set: function(v){
                    this.detail.quantity = dozenExp.fromDozenStr(v)
                }
            }
        },
        methods: {
            addDetail(){
                this.$emit('addOrderDetail', this.detail)
            },
            updateDetail(){
                this.$emit('updateOrderDetail', {
                    detailIndex:this.detailIndex,
                    detail:this.detail
                })
            }
        },
        components: {}
    }
</script>
