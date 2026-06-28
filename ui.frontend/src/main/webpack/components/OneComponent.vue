<template>
   <div>
    <div>one component</div>
    <div>{{ value1 }}</div>
   </div> 
</template>
<script>
export default {
    name: 'MsOne',
    props: {
        value1: {
            type: String,
            default: ''
        }
    },
    mounted() {
        console.log("one component work");
    }
}
</script>
